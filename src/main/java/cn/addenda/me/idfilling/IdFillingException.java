package cn.addenda.me.idfilling;

import cn.addenda.me.MyBatisEasyException;

/**
 * @Author ISJINHAO
 * @Date 2022/2/5 22:45
 */
public class IdFillingException extends MyBatisEasyException {

    public IdFillingException(String message) {
        super(message);
    }

    public IdFillingException(String message, Throwable e) {
        super(message, e);
    }

}
