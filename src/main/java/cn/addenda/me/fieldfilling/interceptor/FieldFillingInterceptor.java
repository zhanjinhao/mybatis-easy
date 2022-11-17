package cn.addenda.me.fieldfilling.interceptor;

import cn.addenda.me.fieldfilling.DefaultFieldFillingContext;
import cn.addenda.me.fieldfilling.FieldFillingContext;
import cn.addenda.me.fieldfilling.FieldFillingException;
import cn.addenda.me.fieldfilling.annotation.DMLFieldFilling;
import cn.addenda.me.fieldfilling.annotation.DQLFieldFilling;
import cn.addenda.me.fieldfilling.sql.FieldFillingConvertor;
import cn.addenda.me.util.MeAnnotationUtils;
import cn.addenda.me.util.MybatisUtils;
import cn.addenda.ro.grammar.ast.CurdUtils;
import cn.addenda.ro.grammar.ast.create.Insert;
import cn.addenda.ro.grammar.ast.expression.Curd;
import cn.addenda.ro.grammar.ast.retrieve.Select;
import cn.addenda.ro.grammar.ast.update.Update;
import cn.addenda.ro.grammar.function.descriptor.FunctionDescriptor;
import cn.addenda.ro.grammar.function.evaluator.DefaultFunctionEvaluator;
import cn.addenda.ro.grammar.function.evaluator.FunctionEvaluator;
import cn.addenda.ro.grammar.lexical.token.Token;
import cn.addenda.ro.util.SqlUtils;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author addenda
 * @datetime 2022/8/16 20:40
 */
@Intercepts({
        @Signature(type = Executor.class, method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class}),
        @Signature(type = Executor.class, method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
})
public class FieldFillingInterceptor implements Interceptor {

    private FieldFillingContext fieldFillingContext;

    private Set<String> tableNameSet;

    private FunctionEvaluator<? extends FunctionDescriptor> functionEvaluator;

    private FieldFillingConvertor fieldFillingConvertor;

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object[] args = invocation.getArgs();

        // 每次进来 ms 对象是同一个
        MappedStatement ms = (MappedStatement) args[0];

        // ms.getBoundSql() 每次都会返回新的对象
        BoundSql boundSql = ms.getBoundSql(args[1]);

        String oldSql = boundSql.getSql();

        String newSql = processSql(oldSql, ms);
        // newSql不为空 且 新旧sql不一致 的才需要进行sql替换
        if (newSql == null || oldSql.replaceAll("\\s+", "").equals(newSql.replaceAll("\\s+", ""))) {
            return invocation.proceed();
        }

        MybatisUtils.executorInterceptorReplaceSql(invocation, newSql);
        return invocation.proceed();
    }

    private String processSql(String oldSql, MappedStatement ms) {
        SqlCommandType sqlCommandType = ms.getSqlCommandType();
        String msId = ms.getId();
        if (SqlCommandType.SELECT.equals(sqlCommandType)) {
            DQLFieldFilling dqlFieldFilling = extractAnnotation(msId, DQLFieldFilling.class);
            return processSelect(oldSql, dqlFieldFilling);
        } else if (SqlCommandType.INSERT.equals(sqlCommandType)) {
            DMLFieldFilling dmlFieldFilling = extractAnnotation(msId, DMLFieldFilling.class);
            return processInsert(oldSql, dmlFieldFilling);
        } else if (SqlCommandType.UPDATE.equals(sqlCommandType)) {
            DMLFieldFilling dmlFieldFilling = extractAnnotation(msId, DMLFieldFilling.class);
            return processUpdate(oldSql, dmlFieldFilling);
        } else if (SqlCommandType.DELETE.equals(sqlCommandType)) {
            // 如果DELETE类型获取到了update sql，认为sql是逻辑删除。
            if (SqlUtils.isUpdateSql(oldSql)) {
                return processUpdate(oldSql, extractAnnotation(msId, DMLFieldFilling.class));
            }
            return oldSql;
        } else {
            throw new FieldFillingException("无法识别的Mybatis SqlCommandType：" + sqlCommandType);
        }
    }

    private String processSelect(String sql, DQLFieldFilling dqlFieldFilling) {
        if (SqlUtils.isSelectSql(sql)) {
            Curd parse = CurdUtils.parse(sql, functionEvaluator, true);
            Select select = (Select) parse;
            if (dqlFieldFilling == null) {
                return fieldFillingConvertor.selectFieldFilling(select, this.tableNameSet, null);
            }
            String[] aTableNameSet = dqlFieldFilling.tableNameSet();
            String masterView = dqlFieldFilling.masterView();
            // java注解不允许空，所以这里转换一下
            if ("".equals(masterView)) {
                masterView = null;
            }
            if (aTableNameSet.length == 1) {
                String mode = aTableNameSet[0];
                if (DQLFieldFilling.ALL.equals(mode)) {
                    return fieldFillingConvertor.selectFieldFilling(select, this.tableNameSet, masterView);
                } else if (DQLFieldFilling.EMPTY.equals(mode)) {
                    return null;
                }
            }
            // 使用自定义的tableNameSet。
            return fieldFillingConvertor.selectFieldFilling(select, new HashSet<>(Arrays.asList(aTableNameSet)), masterView);
        } else {
            throw new FieldFillingException("Mybatis SqlCommandType.SELECT 应该执行 SELECT 语句！");
        }

    }

    private <T extends Annotation> T extractAnnotation(String msId, Class<T> tClazz) {
        int end = msId.lastIndexOf(".");
        try {
            Class<?> aClass = Class.forName(msId.substring(0, end));
            String methodName = msId.substring(end + 1);
            return MeAnnotationUtils.extractAnnotationFromMethod(aClass, methodName, tClazz);
        } catch (ClassNotFoundException e) {
            throw new FieldFillingException("无法找到对应的Mapper：" + msId, e);
        }
    }


    private String processUpdate(String sql, DMLFieldFilling dmlFieldFilling) {
        FieldFillingContext aFieldFillingContext;
        if (dmlFieldFilling == null) {
            aFieldFillingContext = this.fieldFillingContext;
        } else {
            Class<? extends FieldFillingContext> context = dmlFieldFilling.context();
            if (context != null) {
                aFieldFillingContext = newInstance(context);
            } else {
                aFieldFillingContext = this.fieldFillingContext;
            }
        }

        if (SqlUtils.isUpdateSql(sql)) {
            Curd parse = CurdUtils.parse(sql, functionEvaluator, true);
            Update update = (Update) parse;
            Token tableName = update.getTableName();
            if (tableNameSet.contains(String.valueOf(tableName.getLiteral()))) {
                return fieldFillingConvertor.updateFieldFilling(update, aFieldFillingContext);
            }
            return null;
        } else {
            throw new FieldFillingException("Mybatis SqlCommandType.UPDATE 应该执行 update 语句！");
        }
    }

    private String processInsert(String sql, DMLFieldFilling dmlFieldFilling) {
        FieldFillingContext aFieldFillingContext;
        if (dmlFieldFilling == null) {
            aFieldFillingContext = this.fieldFillingContext;
        } else {
            Class<? extends FieldFillingContext> context = dmlFieldFilling.context();
            if (context != null) {
                aFieldFillingContext = newInstance(context);
            } else {
                aFieldFillingContext = this.fieldFillingContext;
            }
        }

        if (SqlUtils.isInsertSql(sql)) {
            Curd parse = CurdUtils.parse(sql, functionEvaluator, true);
            Insert insert = (Insert) parse;
            Token tableName = insert.getTableName();
            if (tableNameSet.contains(String.valueOf(tableName.getLiteral()))) {
                return fieldFillingConvertor.insertFieldFilling(insert, aFieldFillingContext);
            }
            return null;
        } else {
            throw new FieldFillingException("Mybatis SqlCommandType.INSERT 应该执行 INSERT 语句！");
        }
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        String aFieldFillingContext = (String) properties.get("fieldFillingContext");
        if (aFieldFillingContext != null) {
            try {
                Class<?> aClass = Class.forName(aFieldFillingContext);
                if (!FieldFillingContext.class.isAssignableFrom(aClass)) {
                    throw new FieldFillingException(aFieldFillingContext + "不是cn.addenda.me.fieldfilling.FieldFillingContext的子类!");
                }
                this.fieldFillingContext = newInstance((Class<? extends FieldFillingContext>) aClass);
            } catch (Exception e) {
                throw new FieldFillingException("FieldFillingContext初始化失败：" + aFieldFillingContext, e);
            }
        } else {
            this.fieldFillingContext = DefaultFieldFillingContext.getInstance();
        }

        String aTableNameSet = properties.getProperty("tableNameSet");
        if (aTableNameSet != null) {
            this.tableNameSet = Arrays.stream(aTableNameSet.split(",")).collect(Collectors.toSet());
        } else {
            this.tableNameSet = new HashSet<>();
        }

        String aFunctionEvaluator = properties.getProperty("functionEvaluator");
        if (aFunctionEvaluator != null) {
            try {
                Class<?> aClass = Class.forName(aFunctionEvaluator);
                if (!FunctionEvaluator.class.isAssignableFrom(aClass)) {
                    throw new FieldFillingException(aFunctionEvaluator + "不是cn.addenda.ro.grammar.function.evaluator.FunctionEvaluator的子类!");
                }
                this.functionEvaluator = newInstance((Class<? extends FunctionEvaluator>) aClass);
            } catch (Exception e) {
                throw new FieldFillingException("FunctionEvaluator初始化失败：" + aFunctionEvaluator, e);
            }
        } else {
            this.functionEvaluator = DefaultFunctionEvaluator.getInstance();
        }
        this.fieldFillingConvertor = new FieldFillingConvertor(functionEvaluator);
    }

    private <T> T newInstance(Class<? extends T> aClass) {
        try {
            Method[] methods = aClass.getMethods();
            for (Method method : methods) {
                if (method.getName().equals("getInstance") && Modifier.isStatic(method.getModifiers()) &&
                        method.getParameterCount() == 0) {
                    return (T) method.invoke(null);
                }
            }
            return aClass.newInstance();
        } catch (Exception e) {
            throw new FieldFillingException("FieldFillingContext初始化失败：" + aClass.getName(), e);
        }
    }

}
