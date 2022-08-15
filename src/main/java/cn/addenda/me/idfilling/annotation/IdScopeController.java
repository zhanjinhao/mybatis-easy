package cn.addenda.me.idfilling.annotation;

import java.lang.annotation.*;

/**
 * @Author ISJINHAO
 * @Date 2022/2/5 15:57
 */
@Inherited
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface IdScopeController {

    int SUPPRESS = 1;
    int FORCE_INJECT = 2;

    int mode() default SUPPRESS;

}
