package cn.addenda.me.constraint;

/**
 * @author addenda
 * @datetime 2022/11/26 22:36
 */
public class ConstraintException extends RuntimeException {

    public ConstraintException(String message) {
        super(message);
    }

    public ConstraintException(String message, Throwable cause) {
        super(message, cause);
    }
}
