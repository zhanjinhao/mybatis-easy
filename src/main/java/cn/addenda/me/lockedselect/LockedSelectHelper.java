package cn.addenda.me.lockedselect;

/**
 * @author addenda
 * @datetime 2022/10/11 19:14
 */
public class LockedSelectHelper {

    public static final String W_LOCK = "W";
    public static final String R_LOCK = "R";

    private static final ThreadLocal<String> LOCK_THREAD_LOCAL = ThreadLocal.withInitial(() -> null);

    public static void setLock(String lock) {
        if (!W_LOCK.equals(lock) && !R_LOCK.equals(lock)) {
            throw new LockedSelectException("不支持的LOCK类型，当前LOCK类型：" + lock + "。");
        }
        LOCK_THREAD_LOCAL.set(lock);
    }

    public static void clearLock() {
        LOCK_THREAD_LOCAL.remove();
    }

    public static String getLock() {
        return LOCK_THREAD_LOCAL.get();
    }

    public static <T> T rLockedSelect(LockedSqlExecutor<T> executor) {
        try {
            setLock(R_LOCK);
            return executor.execute();
        } catch (Throwable throwable) {
            throw new LockedSelectException(throwable);
        } finally {
            clearLock();
        }
    }

    public static <T> T wLockedSelect(LockedSqlExecutor<T> executor) {
        try {
            setLock(W_LOCK);
            return executor.execute();
        } catch (Throwable throwable) {
            throw new LockedSelectException(throwable);
        } finally {
            clearLock();
        }
    }

    public static <T> T lockSelect(String lock, LockedSqlExecutor<T> executor) {
        if (W_LOCK.equals(lock)) {
            return wLockedSelect(executor);
        } else if (R_LOCK.equals(lock)) {
            return rLockedSelect(executor);
        } else {
            throw new LockedSelectException("不支持的LOCK类型，当前LOCK类型：" + lock + "。");
        }
    }


    public static void rLockedSelect(VoidLockedSqlExecutor executor) {
        try {
            setLock(R_LOCK);
            executor.execute();
        } catch (Throwable throwable) {
            throw new LockedSelectException(throwable);
        } finally {
            clearLock();
        }
    }

    public static void wLockedSelect(VoidLockedSqlExecutor executor) {
        try {
            setLock(W_LOCK);
            executor.execute();
        } catch (Throwable throwable) {
            throw new LockedSelectException(throwable);
        } finally {
            clearLock();
        }
    }

    public static void lockSelect(String lock, VoidLockedSqlExecutor executor) {
        if (W_LOCK.equals(lock)) {
            wLockedSelect(executor);
        } else if (R_LOCK.equals(lock)) {
            rLockedSelect(executor);
        } else {
            throw new LockedSelectException("不支持的LOCK类型，当前LOCK类型：" + lock + "。");
        }
    }

    public interface LockedSqlExecutor<T> {
        T execute() throws Throwable;
    }

    public interface VoidLockedSqlExecutor {
        void execute() throws Throwable;
    }

}
