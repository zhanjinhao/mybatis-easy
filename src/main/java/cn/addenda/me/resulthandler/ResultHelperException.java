package cn.addenda.me.resulthandler;

import cn.addenda.me.MyBatisEasyException;

/**
 * @Author ISJINHAO
 * @Date 2022/2/1 17:33
 */
public class ResultHelperException extends MyBatisEasyException {

    public ResultHelperException(String message) {
        super(message);
    }

    public ResultHelperException(String message, Throwable e) {
        super(message, e);
    }

}
