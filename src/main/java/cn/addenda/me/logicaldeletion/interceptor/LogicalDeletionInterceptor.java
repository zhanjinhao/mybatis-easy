package cn.addenda.me.logicaldeletion.interceptor;

import cn.addenda.me.fieldfilling.FiledFillingException;
import cn.addenda.me.idfilling.IdFillingException;
import cn.addenda.me.logicaldeletion.annotation.LogicalDeletionController;
import cn.addenda.me.logicaldeletion.sql.LogicalDeletionConvertor;
import cn.addenda.me.utils.MeAnnotationUtil;
import cn.addenda.me.utils.MeUtilsException;
import cn.addenda.me.utils.MybatisUtil;
import cn.addenda.ro.grammar.ast.CurdParserFactory;
import cn.addenda.ro.grammar.ast.create.Insert;
import cn.addenda.ro.grammar.ast.delete.Delete;
import cn.addenda.ro.grammar.ast.expression.Curd;
import cn.addenda.ro.grammar.ast.update.Update;
import cn.addenda.ro.grammar.lexical.token.Token;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.lang.reflect.Field;
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

    private static final String LOGICAL_DELETION_TABLE_NAME_SET = "logicalDeletionTableNameSet";
    private final Set<String> logicalDeletionTableNameSet = new HashSet<>();

    private final Map<String, LogicalDeletionController> logicalDeletionControllerMap = new ConcurrentHashMap<>();

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

        BoundSql oldBoundSqlArg = 6 == args.length ? (BoundSql) args[5] : null;
        SqlSource oldSqlSource = ms.getSqlSource();

        boundSqlSetSql(boundSql, newSql);
        SqlSource newSqlSource = newBoundSqlSqlSource(boundSql);

        replaceSql(invocation, boundSql, newSqlSource);
        try {
            return invocation.proceed();
        } finally {
            replaceSql(invocation, oldBoundSqlArg, oldSqlSource);
        }
    }

    private String processSql(String sql, MappedStatement ms) {
        SqlCommandType sqlCommandType = ms.getSqlCommandType();
        if (SqlCommandType.SELECT.equals(sqlCommandType)) {
            return LogicalDeletionConvertor.selectLogically(sql, logicalDeletionTableNameSet);
        } else if (SqlCommandType.INSERT.equals(sqlCommandType)) {
            Curd parse = CurdParserFactory.createCurdParser(sql).parse();
            if (parse instanceof Insert) {
                Insert insert = (Insert) parse;
                Token tableName = insert.getTableName();
                if (logicalDeletionTableNameSet.contains(String.valueOf(tableName.getLiteral()))) {
                    return LogicalDeletionConvertor.insertLogically(insert);
                }
                return null;
            } else {
                throw new FiledFillingException("Mybatis SqlCommandType.INSERT 应该执行 INSERT 语句！");
            }
        } else if (SqlCommandType.UPDATE.equals(sqlCommandType)) {
            Curd parse = CurdParserFactory.createCurdParser(sql).parse();
            if (parse instanceof Update) {
                Update update = (Update) parse;
                Token tableName = update.getTableName();
                if (logicalDeletionTableNameSet.contains(String.valueOf(tableName.getLiteral()))) {
                    return LogicalDeletionConvertor.updateLogically(update);
                }
                return null;
            } else {
                throw new FiledFillingException("Mybatis SqlCommandType.UPDATE 应该执行 UPDATE 语句！");
            }
        } else if (SqlCommandType.DELETE.equals(sqlCommandType)) {
            Curd parse = CurdParserFactory.createCurdParser(sql).parse();
            if (parse instanceof Delete) {
                Delete update = (Delete) parse;
                Token tableName = update.getTableName();
                if (logicalDeletionTableNameSet.contains(String.valueOf(tableName.getLiteral()))) {
                    return LogicalDeletionConvertor.deleteLogically(update);
                }
                return null;
            } else {
                throw new FiledFillingException("Mybatis SqlCommandType.DELETE 应该执行 DELETE 语句！");
            }
        } else {
            throw new FiledFillingException("无法识别的Mybatis SqlCommandType：" + sqlCommandType);
        }
    }

    private LogicalDeletionController extractIdScopeController(String msId) {
        return logicalDeletionControllerMap.computeIfAbsent(msId,
                s -> {
                    int end = msId.lastIndexOf(".");
                    try {
                        Class<?> aClass = Class.forName(msId.substring(0, end));
                        String methodName = msId.substring(end + 1);
                        return MeAnnotationUtil.extractAnnotationFromMethod(aClass, methodName, LogicalDeletionController.class);
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
        String tableNameSet = properties.getProperty(LOGICAL_DELETION_TABLE_NAME_SET);
        if (tableNameSet == null) {
            return;
        }
        logicalDeletionTableNameSet.addAll(Arrays.stream(tableNameSet.split(",")).collect(Collectors.toSet()));
    }

    public static void replaceSql(Invocation invocation, BoundSql boundSql, SqlSource sqlSource) {
        final Object[] args = invocation.getArgs();
        MappedStatement statement = (MappedStatement) args[0];
        MetaObject msObject = SystemMetaObject.forObject(statement);
        msObject.setValue("sqlSource", sqlSource);

        // 如果参数个数为6，还需要处理 BoundSql 对象
        if (6 == args.length) {
            args[5] = boundSql;
        }
    }

    public SqlSource newBoundSqlSqlSource(BoundSql boundSql) {
        return new BoundSqlSqlSource(boundSql);
    }

    public void boundSqlSetSql(BoundSql boundSql, String sql) {
        // 该对象没有提供对sql属性的set方法，只能通过反射进行修改
        Class<? extends BoundSql> aClass = boundSql.getClass();
        try {
            Field field = aClass.getDeclaredField("sql");
            field.setAccessible(true);
            field.set(boundSql, sql);
        } catch (Exception e) {
            throw new MeUtilsException("替换 BoundSql.sql 失败！", e);
        }
    }

    private static class BoundSqlSqlSource implements SqlSource {

        private final BoundSql boundSql;

        public BoundSqlSqlSource(BoundSql boundSql) {
            this.boundSql = boundSql;
        }

        public BoundSql getBoundSql(Object parameterObject) {
            return boundSql;
        }
    }

}
