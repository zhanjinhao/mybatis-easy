package cn.addenda.me.fieldfilling.annotation;

import java.lang.annotation.*;

@Inherited
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface DQLFieldFilling {

    /**
     * 此属性为true时，independent 和 availableTableNames 会被覆盖为 true 和 ""。
     */
    boolean allTableNameAvailable() default false;

    String availableTableNames() default "";

    boolean independent() default false;

}
