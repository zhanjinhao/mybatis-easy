package cn.addenda.me.logicaldeletion.annotation;

import java.lang.annotation.*;

@Inherited
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface LogicalDeletionController {

    boolean suppress() default true;

}
