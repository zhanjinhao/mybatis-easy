package cn.addenda.me.fieldfilling.annotation;

import java.lang.annotation.*;

@Inherited
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface DMLFieldFilling {

    String fieldFillingContextClazzName() default "";

}
