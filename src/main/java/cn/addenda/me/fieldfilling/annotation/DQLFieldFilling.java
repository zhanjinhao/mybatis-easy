package cn.addenda.me.fieldfilling.annotation;

import java.lang.annotation.*;

import static cn.addenda.me.constant.Constants.ALL;

@Inherited
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface DQLFieldFilling {

    /**
     * mapper的方法上不存在DQLFieldFilling时，取的是拦截器里配置的表名，默认的ALL模式与此一致。
     */
    String[] tableNameSet() default ALL;

    String masterView() default "";

}
