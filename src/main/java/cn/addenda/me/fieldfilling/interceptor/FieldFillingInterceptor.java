package cn.addenda.me.fieldfilling.interceptor;

import cn.addenda.me.fieldfilling.DefaultFieldFillingContext;
import cn.addenda.me.fieldfilling.FieldFillingContext;
import cn.addenda.me.fieldfilling.FieldFillingException;
import cn.addenda.me.fieldfilling.annotation.DMLFieldFilling;
import cn.addenda.me.fieldfilling.annotation.DQLFieldFilling;
import cn.addenda.me.fieldfilling.sql.FieldFillingConvertor;
import cn.addenda.me.utils.MeAnnotationUtils;
import cn.addenda.me.utils.MybatisUtils;
import cn.addenda.ro.grammar.ast.CurdUtils;
import cn.addenda.ro.grammar.ast.create.Insert;
import cn.addenda.ro.grammar.ast.expression.Curd;
import cn.addenda.ro.grammar.ast.retrieve.Select;
import cn.addenda.ro.grammar.ast.update.Update;
import cn.addenda.ro.grammar.lexical.token.Token;
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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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

    private static final String DEFAULT_FIELD_FILLING_CONTEXT_NAME = "defaultFieldFillingContext";
    private FieldFillingContext defaultFieldFillingContext;

    private static final String FIELD_FILLING_TABLE_NAME_SET = "fieldFillingTableNameSet";
    private final Set<String> fieldFillingTableNameSet = new HashSet<>();

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object[] args = invocation.getArgs();

        // 每次进来 ms 对象是同一个
        MappedStatement ms = (MappedStatement) args[0];

        // ms.getBoundSql() 每次都会返回新的对象
        BoundSql boundSql = ms.getBoundSql(args[1]);

        String oldSql = boundSql.getSql();

        String msId = ms.getId();

        String newSql = processSql(oldSql, ms);
        // newSql不为空 且 新旧sql不一致 的才需要进行sql替换
        if (newSql == null || oldSql.replaceAll("\\s+", "").equals(newSql.replaceAll("\\s+", ""))) {
            return invocation.proceed();
        }

        MybatisUtils.executorInterceptorReplaceSql(invocation, boundSql, newSql);
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
            return oldSql;
        } else {
            throw new FieldFillingException("无法识别的Mybatis SqlCommandType：" + sqlCommandType);
        }
    }

    private String processSelect(String sql, DQLFieldFilling dqlFieldFilling) {
        Curd parse = CurdUtils.parse(sql, true);
        if (parse instanceof Select) {
            Select select = (Select) parse;
            if (dqlFieldFilling == null) {
                return FieldFillingConvertor.selectFieldFilling(select, fieldFillingTableNameSet);
            }
            String[] tableNameSet = dqlFieldFilling.tableNameSet();
            if (tableNameSet.length == 1) {
                String mode = tableNameSet[0];
                if (DQLFieldFilling.ALL.equals(mode)) {
                    return FieldFillingConvertor.selectFieldFilling(select, fieldFillingTableNameSet);
                } else if (DQLFieldFilling.IGNORE.equals(mode)) {
                    return null;
                }
            }
            return FieldFillingConvertor.selectFieldFilling(select, new HashSet<>(Arrays.asList(tableNameSet)));
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
        FieldFillingContext fieldFillingContext;
        if (dmlFieldFilling == null) {
            fieldFillingContext = defaultFieldFillingContext;
        } else {
            Class<? extends FieldFillingContext> context = dmlFieldFilling.context();
            if (context != null) {
                fieldFillingContext = newInstance(context);
            } else {
                fieldFillingContext = defaultFieldFillingContext;
            }
        }

        Curd parse = CurdUtils.parse(sql, true);
        if (parse instanceof Update) {
            Update update = (Update) parse;
            Token tableName = update.getTableName();
            if (fieldFillingTableNameSet.contains(String.valueOf(tableName.getLiteral()))) {
                return FieldFillingConvertor.updateFieldFilling(update, fieldFillingContext);
            }
            return null;
        } else {
            throw new FieldFillingException("Mybatis SqlCommandType.UPDATE 应该执行 update 语句！");
        }
    }

    private String processInsert(String sql, DMLFieldFilling dmlFieldFilling) {
        FieldFillingContext fieldFillingContext;
        if (dmlFieldFilling == null) {
            fieldFillingContext = defaultFieldFillingContext;
        } else {
            Class<? extends FieldFillingContext> context = dmlFieldFilling.context();
            if (context != null) {
                fieldFillingContext = newInstance(context);
            } else {
                fieldFillingContext = defaultFieldFillingContext;
            }
        }

        Curd parse = CurdUtils.parse(sql, true);
        if (parse instanceof Insert) {
            Insert insert = (Insert) parse;
            Token tableName = insert.getTableName();
            if (fieldFillingTableNameSet.contains(String.valueOf(tableName.getLiteral()))) {
                return FieldFillingConvertor.insertFieldFilling(insert, fieldFillingContext);
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
        String defaultFieldFillingContextNameValue = (String) properties.get(DEFAULT_FIELD_FILLING_CONTEXT_NAME);
        if (defaultFieldFillingContextNameValue != null) {
            this.defaultFieldFillingContext = newInstance(defaultFieldFillingContextNameValue);
        }

        String tableNameSet = properties.getProperty(FIELD_FILLING_TABLE_NAME_SET);
        if (tableNameSet != null) {
            fieldFillingTableNameSet.addAll(Arrays.stream(tableNameSet.split(",")).collect(Collectors.toSet()));
        }
    }

    private FieldFillingContext newInstance(String clazzName) {
        if (clazzName == null) {
            return DefaultFieldFillingContext.getInstance();
        }
        try {
            Class<?> aClass = Class.forName(clazzName);
            if (!FieldFillingContext.class.isAssignableFrom(aClass)) {
                throw new FieldFillingException("FieldFillingContext初始化失败：" + clazzName + "需要是cn.addenda.me.fieldfilling.FieldFillingContext的子类!");
            }
            return newInstance((Class<? extends FieldFillingContext>) aClass);
        } catch (Exception e) {
            throw new FieldFillingException("FieldFillingContext初始化失败：" + clazzName, e);
        }
    }

    private final Map<Class<? extends FieldFillingContext>, FieldFillingContext> fieldFillingContextMap = new ConcurrentHashMap<>();

    private FieldFillingContext newInstance(Class<? extends FieldFillingContext> aClass) {
        return fieldFillingContextMap.computeIfAbsent(aClass, s -> {
            try {
                Method[] methods = aClass.getMethods();
                for (Method method : methods) {
                    if (method.getName().equals("getInstance") && Modifier.isStatic(method.getModifiers()) &&
                            method.getParameterCount() == 0 && FieldFillingContext.class.isAssignableFrom(method.getReturnType())) {
                        return (FieldFillingContext) method.invoke(null);
                    }
                }
                return aClass.newInstance();
            } catch (Exception e) {
                throw new FieldFillingException("FieldFillingContext初始化失败：" + aClass.getName(), e);
            }
        });
    }

}
