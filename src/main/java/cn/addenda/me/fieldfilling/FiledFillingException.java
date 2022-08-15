package cn.addenda.me.fieldfilling;

import cn.addenda.me.MyBatisEasyException;

/**
 * @Author ISJINHAO
 * @Date 2022/2/1 17:33
 */
public class FiledFillingException extends MyBatisEasyException {

    public FiledFillingException(String message) {
        super(message);
    }

    public FiledFillingException(String message, Throwable e) {
        super(message, e);
    }

}
