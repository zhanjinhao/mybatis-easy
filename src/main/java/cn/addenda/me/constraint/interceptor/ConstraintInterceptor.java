package cn.addenda.me.constraint.interceptor;

import cn.addenda.me.constant.Constants;
import cn.addenda.me.constraint.ConstraintContext;
import cn.addenda.me.constraint.ConstraintException;
import cn.addenda.me.constraint.annotation.TableConstraint;
import cn.addenda.me.constraint.annotation.ViewConstraint;
import cn.addenda.me.fieldfilling.FieldFillingException;
import cn.addenda.me.util.MeAnnotationUtils;
import cn.addenda.me.util.MybatisUtils;
import cn.addenda.ro.grammar.function.descriptor.FunctionDescriptor;
import cn.addenda.ro.grammar.function.evaluator.DefaultFunctionEvaluator;
import cn.addenda.ro.grammar.function.evaluator.FunctionEvaluator;
import cn.addenda.ro.util.SqlAddConditionUtils;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 这个插件改写sql之后命中的数据范围与原sql不同，对于PageHelper这样的插件需要保证自己组装的sql（count()）也要经过改写。
 *
 * @author addenda
 * @datetime 2022/11/26 21:04
 */
@Intercepts({
        @Signature(type = Executor.class, method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class}),
        @Signature(type = Executor.class, method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
})
public class ConstraintInterceptor implements Interceptor {

    private final Map<String, TableConstraint> tableConstraintMap = new ConcurrentHashMap<>();
    private final Map<String, ViewConstraint> viewConstraintMap = new ConcurrentHashMap<>();

    private FunctionEvaluator<? extends FunctionDescriptor> functionEvaluator;

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        BoundSql boundSql = MybatisUtils.getBoundSql(invocation);
        String oldSql = boundSql.getSql();
        MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
        String msId = ms.getId();

        String newSql = oldSql;

        // view 过滤条件
        Map<String, String> viewConstraints = ConstraintContext.getViewConstraints();
        if (viewConstraints != null && !viewConstraints.isEmpty()) {
            ViewConstraint viewConstraint = extractViewConstraint(msId);
            if (viewConstraint != null) {
                String viewSetStr = viewConstraint.viewSet();
                newSql = apply(oldSql, viewSetStr, viewConstraints, SqlAddConditionUtils::addViewCondition);
            }
        }

        // table 过滤条件
        Map<String, String> tableConstraints = ConstraintContext.getTableConstraints();
        if (tableConstraints != null && !tableConstraints.isEmpty()) {
            // 如果不是ALL的话，就用Mapper上的注解过滤上下文里的表
            TableConstraint tableConstraint = extractTableConstraint(msId);
            if (tableConstraint != null) {
                String tableSetStr = tableConstraint.tableSet();
                newSql = apply(oldSql, tableSetStr, tableConstraints, SqlAddConditionUtils::addTableCondition);
            }
        }

        if (!newSql.equals(oldSql)) {
            MybatisUtils.executorInterceptorReplaceSql(invocation, newSql);
        }

        return invocation.proceed();
    }

    private String apply(String sql, String setStr, Map<String, String> constraints, QuaternionOperator<String, FunctionEvaluator<?>> operator) {
        // 如果是EMPTY，直接返回就行
        if (Constants.EMPTY.equals(setStr)) {
            return sql;
        }
        String newSql = sql;
        Map<String, String> deepClone = new LinkedHashMap<>(constraints);
        // 如果不是ALL的话，就用Mapper上的注解过滤上下文里的表
        if (!Constants.ALL.equals(setStr)) {
            Set<String> set = Arrays.stream(setStr.split(",")).collect(Collectors.toSet());
            deepClone.entrySet().removeIf(tableConstraint -> !set.contains(tableConstraint.getKey()));
        }
        Set<Map.Entry<String, String>> entries = deepClone.entrySet();
        for (Map.Entry<String, String> entry : entries) {
            newSql = operator.apply(newSql, entry.getKey(), entry.getValue(), functionEvaluator);
        }
        return newSql;
    }

    private TableConstraint extractTableConstraint(String msId) {
        return tableConstraintMap.computeIfAbsent(msId,
                s -> {
                    int end = msId.lastIndexOf(".");
                    try {
                        Class<?> aClass = Class.forName(msId.substring(0, end));
                        String methodName = msId.substring(end + 1);
                        return MeAnnotationUtils.extractAnnotationFromMethod(aClass, methodName, TableConstraint.class);
                    } catch (ClassNotFoundException e) {
                        throw new ConstraintException("无法找到对应的Mapper：" + msId, e);
                    }
                });
    }

    private ViewConstraint extractViewConstraint(String msId) {
        return viewConstraintMap.computeIfAbsent(msId,
                s -> {
                    int end = msId.lastIndexOf(".");
                    try {
                        Class<?> aClass = Class.forName(msId.substring(0, end));
                        String methodName = msId.substring(end + 1);
                        return MeAnnotationUtils.extractAnnotationFromMethod(aClass, methodName, ViewConstraint.class);
                    } catch (ClassNotFoundException e) {
                        throw new ConstraintException("无法找到对应的Mapper：" + msId, e);
                    }
                });
    }

    @Override
    public void setProperties(Properties properties) {
        String aFunctionEvaluator = properties.getProperty("functionEvaluator");
        if (aFunctionEvaluator != null) {
            try {
                Class<?> aClass = Class.forName(aFunctionEvaluator);
                if (!FunctionEvaluator.class.isAssignableFrom(aClass)) {
                    throw new FieldFillingException(aFunctionEvaluator + "不是cn.addenda.ro.grammar.function.evaluator.FunctionEvaluator的子类!");
                }
                this.functionEvaluator = newInstance((Class<? extends FunctionEvaluator>) aClass);
            } catch (Exception e) {
                throw new FieldFillingException("FunctionEvaluator初始化失败：" + aFunctionEvaluator, e);
            }
        } else {
            this.functionEvaluator = DefaultFunctionEvaluator.getInstance();
        }
    }

    private <T> T newInstance(Class<? extends T> aClass) {
        try {
            Method[] methods = aClass.getMethods();
            for (Method method : methods) {
                if (method.getName().equals("getInstance") && Modifier.isStatic(method.getModifiers()) &&
                        method.getParameterCount() == 0) {
                    return (T) method.invoke(null);
                }
            }
            return aClass.newInstance();
        } catch (Exception e) {
            throw new ConstraintException("FieldFillingContext初始化失败：" + aClass.getName(), e);
        }
    }

    private interface QuaternionOperator<T, F> {
        T apply(T t1, T t2, T t3, F f);
    }

}
