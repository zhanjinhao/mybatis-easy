package cn.addenda.me.utils;

import cn.addenda.me.MyBatisEasyException;

/**
 * @author addenda
 * @datetime 2022/8/7 12:29
 */
public class MeUtilsException extends MyBatisEasyException {
    public MeUtilsException(String message) {
        super(message);
    }

    public MeUtilsException(String message, Throwable cause) {
        super(message, cause);
    }
}
