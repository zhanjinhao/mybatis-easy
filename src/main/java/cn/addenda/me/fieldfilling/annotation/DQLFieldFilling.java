package cn.addenda.me.fieldfilling.annotation;

import java.lang.annotation.*;

@Inherited
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface DQLFieldFilling {

    /**
     * mapper的方法上不存在DQLFieldFilling时，取的是拦截器里配置的表名，默认的ALL模式与此一致。
     */
    String ALL = "all";

    /**
     * 为空时表示不进行增强
     */
    String EMPTY = "empty";

    String[] tableNameSet() default ALL;

    String masterView();

}
