package cn.addenda.me.constraint.annotation;

import java.lang.annotation.*;

import static cn.addenda.me.constant.Constants.EMPTY;

/**
 * @author addenda
 * @datetime 2022/11/26 20:56
 */
@Inherited
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface TableConstraint {

    String tableSet() default EMPTY;

}
