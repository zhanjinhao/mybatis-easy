package cn.addenda.me.utils;

import org.apache.ibatis.executor.BatchExecutor;
import org.apache.ibatis.executor.CachingExecutor;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.SimpleExecutor;

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

}
