package cn.addenda.me.idfilling.annotation;

import java.lang.annotation.*;

/**
 * @Author ISJINHAO
 * @Date 2022/2/3 20:34
 */
@Inherited
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface IdScope {

    String scopeName();

    String idFieldName() default "id";

}
