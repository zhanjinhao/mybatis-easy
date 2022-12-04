package cn.addenda.me.constraint.annotation;

import java.lang.annotation.*;

import static cn.addenda.me.constant.Constants.EMPTY;

/**
 * @author addenda
 * @datetime 2022/11/26 20:58
 */
@Inherited
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ViewConstraint {

    String viewSet() default EMPTY;

}
