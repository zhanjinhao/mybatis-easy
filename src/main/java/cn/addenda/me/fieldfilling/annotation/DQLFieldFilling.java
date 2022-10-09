package cn.addenda.me.fieldfilling.annotation;

import java.lang.annotation.*;

@Inherited
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface DQLFieldFilling {

    String ALL = "all";

    String EMPTY = "empty";

    String[] tableNameSet() default ALL;

}
