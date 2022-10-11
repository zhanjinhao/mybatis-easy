package cn.addenda.me.lockedselect;

import cn.addenda.me.MyBatisEasyException;

/**
 * @author addenda
 * @datetime 2022/10/11 19:19
 */
public class LockedSelectException extends MyBatisEasyException {

    public LockedSelectException(String message) {
        super(message);
    }

    public LockedSelectException(String message, Throwable cause) {
        super(message, cause);
    }

    public LockedSelectException(Throwable cause) {
        super(cause);
    }

}
