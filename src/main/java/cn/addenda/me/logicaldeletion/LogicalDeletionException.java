package cn.addenda.me.logicaldeletion;

import cn.addenda.me.MyBatisEasyException;

/**
 * @author addenda
 * @datetime 2022/8/16 20:35
 */
public class LogicalDeletionException extends MyBatisEasyException {

    public LogicalDeletionException(String message) {
        super(message);
    }

}
