package cn.addenda.me.fieldfilling;

import cn.addenda.me.MyBatisEasyException;

/**
 * @Author ISJINHAO
 * @Date 2022/2/1 17:33
 */
public class FieldFillingException extends MyBatisEasyException {

    public FieldFillingException(String message) {
        super(message);
    }

    public FieldFillingException(String message, Throwable e) {
        super(message, e);
    }

}
