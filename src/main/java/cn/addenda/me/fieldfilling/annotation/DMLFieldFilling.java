package cn.addenda.me.fieldfilling.annotation;

import cn.addenda.me.fieldfilling.DefaultFieldFillingContext;
import cn.addenda.me.fieldfilling.FieldFillingContext;

import java.lang.annotation.*;

@Inherited
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface DMLFieldFilling {

    Class<? extends FieldFillingContext> context() default DefaultFieldFillingContext.class;

}
