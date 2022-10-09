package cn.addenda.me.logicaldeletion.interceptor;

import cn.addenda.me.fieldfilling.FieldFillingException;
import cn.addenda.me.idfilling.IdFillingException;
import cn.addenda.me.logicaldeletion.LogicalDeletionException;
import cn.addenda.me.logicaldeletion.annotation.LogicalDeletionController;
import cn.addenda.me.logicaldeletion.sql.LogicalDeletionConvertor;
import cn.addenda.me.utils.MeAnnotationUtils;
import cn.addenda.me.utils.MybatisUtils;
import cn.addenda.ro.grammar.ast.CurdUtils;
import cn.addenda.ro.grammar.ast.create.Insert;
import cn.addenda.ro.grammar.ast.delete.Delete;
import cn.addenda.ro.grammar.ast.expression.Curd;
import cn.addenda.ro.grammar.ast.update.Update;
import cn.addenda.ro.grammar.function.descriptor.FunctionDescriptor;
import cn.addenda.ro.grammar.function.evaluator.DefaultFunctionEvaluator;
import cn.addenda.ro.grammar.function.evaluator.FunctionEvaluator;
import cn.addenda.ro.grammar.lexical.token.Token;
import cn.addenda.ro.utils.SqlUtils;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author addenda
 * @datetime 2022/8/16 21:03
 */
@Intercepts({
        @Signature(type = Executor.class, method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class}),
        @Signature(type = Executor.class, method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
})
public class LogicalDeletionInterceptor implements Interceptor {

    private final Set<String> logicalDeletionTableNameSet = new HashSet<>();

    private final Map<String, LogicalDeletionController> logicalDeletionControllerMap = new ConcurrentHashMap<>();

    private FunctionEvaluator<? extends FunctionDescriptor> functionEvaluator;

    private LogicalDeletionConvertor logicalDeletionConvertor;

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object[] args = invocation.getArgs();
        MappedStatement ms = (MappedStatement) args[0];
        BoundSql boundSql = ms.getBoundSql(args[1]);
        String oldSql = boundSql.getSql();

        String msId = ms.getId();
        LogicalDeletionController logicalDeletionController = extractIdScopeController(msId);
        if (logicalDeletionController != null && logicalDeletionController.suppress()) {
            return invocation.proceed();
        }

        String newSql = processSql(oldSql, ms);
        // newSql不为空 且 新旧sql不一致 的才需要进行sql替换
        if (newSql == null || oldSql.replaceAll("\\s+", "").equals(newSql.replaceAll("\\s+", ""))) {
            return invocation.proceed();
        }

        MybatisUtils.executorInterceptorReplaceSql(invocation, boundSql, newSql);
        return invocation.proceed();
    }

    private String processSql(String sql, MappedStatement ms) {
        SqlCommandType sqlCommandType = ms.getSqlCommandType();
        if (SqlCommandType.SELECT.equals(sqlCommandType)) {
            return logicalDeletionConvertor.selectLogically(sql, logicalDeletionTableNameSet);
        } else if (SqlCommandType.INSERT.equals(sqlCommandType)) {
            if (SqlUtils.isInsertSql(sql)) {
                Curd parse = CurdUtils.parse(sql, functionEvaluator, true);
                Insert insert = (Insert) parse;
                Token tableName = insert.getTableName();
                if (logicalDeletionTableNameSet.contains(String.valueOf(tableName.getLiteral()))) {
                    return logicalDeletionConvertor.insertLogically(insert);
                }
                return null;
            } else {
                throw new LogicalDeletionException("Mybatis SqlCommandType.INSERT 应该执行 INSERT 语句！");
            }
        } else if (SqlCommandType.UPDATE.equals(sqlCommandType)) {
            if (SqlUtils.isUpdateSql(sql)) {
                Curd parse = CurdUtils.parse(sql, functionEvaluator, true);
                Update update = (Update) parse;
                Token tableName = update.getTableName();
                if (logicalDeletionTableNameSet.contains(String.valueOf(tableName.getLiteral()))) {
                    return logicalDeletionConvertor.updateLogically(update);
                }
                return null;
            } else {
                throw new LogicalDeletionException("Mybatis SqlCommandType.UPDATE 应该执行 UPDATE 语句！");
            }
        } else if (SqlCommandType.DELETE.equals(sqlCommandType)) {
            if (SqlUtils.isDeleteSql(sql)) {
                Curd parse = CurdUtils.parse(sql, functionEvaluator, true);
                Delete update = (Delete) parse;
                Token tableName = update.getTableName();
                if (logicalDeletionTableNameSet.contains(String.valueOf(tableName.getLiteral()))) {
                    return logicalDeletionConvertor.deleteLogically(update);
                }
                return null;
            } else {
                throw new LogicalDeletionException("Mybatis SqlCommandType.DELETE 应该执行 DELETE 语句！");
            }
        } else {
            throw new LogicalDeletionException("无法识别的Mybatis SqlCommandType：" + sqlCommandType);
        }
    }

    private LogicalDeletionController extractIdScopeController(String msId) {
        return logicalDeletionControllerMap.computeIfAbsent(msId,
                s -> {
                    int end = msId.lastIndexOf(".");
                    try {
                        Class<?> aClass = Class.forName(msId.substring(0, end));
                        String methodName = msId.substring(end + 1);
                        return MeAnnotationUtils.extractAnnotationFromMethod(aClass, methodName, LogicalDeletionController.class);
                    } catch (ClassNotFoundException e) {
                        throw new IdFillingException("无法找到对应的Mapper：" + msId, e);
                    }
                });
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        String tableNameSet = properties.getProperty("tableNameSet");
        if (tableNameSet == null) {
            return;
        }
        logicalDeletionTableNameSet.addAll(Arrays.stream(tableNameSet.split(",")).collect(Collectors.toSet()));

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

        logicalDeletionConvertor = new LogicalDeletionConvertor(functionEvaluator);
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
