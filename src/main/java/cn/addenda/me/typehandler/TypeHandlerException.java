package cn.addenda.me.typehandler;

import cn.addenda.me.MyBatisEasyException;

/**
 * @author addenda
 * @datetime 2022/8/7 12:30
 */
public class TypeHandlerException extends MyBatisEasyException {
    public TypeHandlerException(String message) {
        super(message);
    }

    public TypeHandlerException(String message, Throwable cause) {
        super(message, cause);
    }
}
