package cn.addenda.me.lockedselect.interceptor;

import cn.addenda.me.lockedselect.LockedSelectException;
import cn.addenda.me.lockedselect.LockedSelectHelper;
import cn.addenda.me.utils.MybatisUtils;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

/**
 * @author addenda
 * @datetime 2022/10/11 19:19
 */
@Intercepts({
        @Signature(type = Executor.class, method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class}),
        @Signature(type = Executor.class, method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class})
})
public class LockedSelectInterceptor implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object[] args = invocation.getArgs();
        MappedStatement ms = (MappedStatement) args[0];
        BoundSql boundSql = ms.getBoundSql(args[1]);
        String oldSql = boundSql.getSql();

        String lock = LockedSelectHelper.getLock();
        if (lock == null) {
            return invocation.proceed();
        }

        String newSql = processSql(oldSql, lock);

        MybatisUtils.executorInterceptorReplaceSql(invocation, boundSql, newSql);
        return invocation.proceed();
    }

    private String processSql(String oldSql, String lock) {
        if (LockedSelectHelper.R_LOCK.equals(lock)) {
            return oldSql + " lock in share mode";
        } else if (LockedSelectHelper.W_LOCK.equals(lock)) {
            return oldSql + " for update";
        }
        throw new LockedSelectException("不支持的LOCK类型，当前LOCK类型：" + lock + "。");
    }
}
