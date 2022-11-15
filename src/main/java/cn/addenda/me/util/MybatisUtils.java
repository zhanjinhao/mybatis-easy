package cn.addenda.me.util;

import cn.addenda.me.MyBatisEasyException;
import org.apache.ibatis.executor.BatchExecutor;
import org.apache.ibatis.executor.CachingExecutor;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.SimpleExecutor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

import java.lang.reflect.Field;

/**
 * @Author ISJINHAO
 * @Date 2022/4/15 18:28
 */
public class MybatisUtils {

    private MybatisUtils() {
    }

    public static boolean isFinallySimpleExecutor(Executor executor) {
        if (executor instanceof SimpleExecutor) {
            return true;
        }
        if (executor instanceof BatchExecutor) {
            return false;
        }
        if (executor instanceof CachingExecutor) {
            try {
                Field delegate = CachingExecutor.class.getDeclaredField("delegate");
                delegate.setAccessible(true);
                Object o = delegate.get(executor);
                return isFinallySimpleExecutor((Executor) o);
            } catch (IllegalAccessException | NoSuchFieldException e) {
                throw new MyBatisEasyException("无法从 CachingExecutor 中获取到 delegate。当前Executor: " + executor.getClass() + ".", e);
            }
        }
        throw new MyBatisEasyException("只支持 SimpleExecutor、BatchExecutor和CachingExecutor! 当前是：" + executor.getClass() + ".");
    }

    public static MappedStatement cloneMappedStatement(MappedStatement ms) {
        MappedStatement.Builder builder =
                new MappedStatement.Builder(ms.getConfiguration(), ms.getId(), ms.getSqlSource(), ms.getSqlCommandType());
        builder.resource(ms.getResource());
        builder.parameterMap(ms.getParameterMap());
        builder.resultMaps(ms.getResultMaps());
        builder.fetchSize(ms.getFetchSize());
        builder.timeout(ms.getTimeout());
        builder.statementType(ms.getStatementType());
        builder.resultSetType(ms.getResultSetType());
        builder.cache(ms.getCache());
        builder.flushCacheRequired(ms.isFlushCacheRequired());
        builder.useCache(ms.isUseCache());
        builder.resultOrdered(ms.isResultOrdered());

        builder.keyGenerator(ms.getKeyGenerator());
        String[] keyProperties = ms.getKeyProperties();
        if (keyProperties != null) {
            builder.keyProperty(String.join(",", keyProperties));
        }
        String[] keyColumns = ms.getKeyColumns();
        if (keyColumns != null) {
            builder.keyColumn(String.join(",", keyColumns));
        }

        builder.databaseId(ms.getDatabaseId());
        builder.lang(ms.getLang());
        String[] resultSets = ms.getResultSets();
        if (resultSets != null) {
            builder.resultSets(String.join(",", resultSets));
        }

        return builder.build();
    }

    public static BoundSql getBoundSql(Invocation invocation) {
        Object[] args = invocation.getArgs();
        MappedStatement ms = (MappedStatement) args[0];
        return ms.getBoundSql(args[1]);
    }

    /**
     * @param invocation
     * @param newSql
     */
    public static void executorInterceptorReplaceSql(Invocation invocation, String newSql) {
        BoundSql boundSql = getBoundSql(invocation);

        MybatisUtils.boundSqlSetSql(boundSql, newSql);
        SqlSource newSqlSource = MybatisUtils.newBoundSqlSqlSource(boundSql);

        final Object[] args = invocation.getArgs();
        // Interceptor 拦截到的 ms 是同一个对象，
        // 这里需要clone一份，再进行属性替换。否则会影响到其他的执行。
        MappedStatement statement = cloneMappedStatement((MappedStatement) args[0]);
        MetaObject msObject = SystemMetaObject.forObject(statement);
        msObject.setValue("sqlSource", newSqlSource);
        args[0] = statement;

        // 如果参数个数为6，还需要处理 BoundSql 对象
        if (6 == args.length) {
            args[5] = boundSql;
        }
    }

    public static void boundSqlSetSql(BoundSql boundSql, String sql) {
        // 该对象没有提供对sql属性的set方法，只能通过反射进行修改
        Class<? extends BoundSql> aClass = boundSql.getClass();
        try {
            Field field = aClass.getDeclaredField("sql");
            field.setAccessible(true);
            field.set(boundSql, sql);
        } catch (Exception e) {
            throw new MyBatisEasyException("替换 BoundSql.sql 失败！", e);
        }
    }

    public static SqlSource newBoundSqlSqlSource(BoundSql boundSql) {
        return new BoundSqlSqlSource(boundSql);
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
