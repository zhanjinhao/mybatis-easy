package cn.addenda.me;

/**
 * @author addenda
 * @datetime 2022/8/7 12:25
 */
public class MyBatisEasyException extends RuntimeException {

    public MyBatisEasyException(String message) {
        super(message);
    }

    public MyBatisEasyException(String message, Throwable cause) {
        super(message, cause);
    }

    public MyBatisEasyException(Throwable cause) {
        super(cause);
    }

}
