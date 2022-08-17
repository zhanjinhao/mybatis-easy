package cn.addenda.me.utils;

import org.apache.ibatis.executor.BatchExecutor;
import org.apache.ibatis.executor.CachingExecutor;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.SimpleExecutor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;

import java.lang.reflect.Field;

/**
 * @Author ISJINHAO
 * @Date 2022/4/15 18:28
 */
public class MybatisUtil {

    private MybatisUtil() {
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
                throw new MeUtilsException("无法从 CachingExecutor 中获取到 delegate。当前Executor: " + executor.getClass() + ".", e);
            }
        }
        throw new MeUtilsException("只支持 SimpleExecutor、BatchExecutor和CachingExecutor! 当前是：" + executor.getClass() + ".");
    }

    /**
     * @param ms           旧的MappedStatement
     * @param newSqlSource 改写后SQL的SqlSource
     * @return
     */
    public static MappedStatement newMappedStatement(MappedStatement ms, SqlSource newSqlSource) {
        MappedStatement.Builder builder =
                new MappedStatement.Builder(ms.getConfiguration(), ms.getId(), newSqlSource, ms.getSqlCommandType());
        builder.resource(ms.getResource());
        builder.fetchSize(ms.getFetchSize());
        builder.statementType(ms.getStatementType());
        builder.keyGenerator(ms.getKeyGenerator());
        if (ms.getKeyProperties() != null && ms.getKeyProperties().length != 0) {
            StringBuilder keyProperties = new StringBuilder();
            for (String keyProperty : ms.getKeyProperties()) {
                keyProperties.append(keyProperty).append(",");
            }
            keyProperties.delete(keyProperties.length() - 1, keyProperties.length());
            builder.keyProperty(keyProperties.toString());
        }
        builder.timeout(ms.getTimeout());
        builder.parameterMap(ms.getParameterMap());
        builder.resultMaps(ms.getResultMaps());
        builder.resultSetType(ms.getResultSetType());
        builder.cache(ms.getCache());
        builder.flushCacheRequired(ms.isFlushCacheRequired());
        builder.useCache(ms.isUseCache());

        return builder.build();
    }

}
