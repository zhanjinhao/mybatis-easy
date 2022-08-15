package cn.addenda.me.idfilling.interceptor;

import cn.addenda.me.idfilling.IdFillingException;
import cn.addenda.me.idfilling.annotation.IdScope;
import cn.addenda.me.idfilling.annotation.IdScopeController;
import cn.addenda.me.idfilling.idgenerator.IdGenerator;
import cn.addenda.me.utils.MeAnnotationUtil;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author ISJINHAO
 * @Date 2022/2/3 20:17
 */
@Intercepts({
        @Signature(type = Executor.class, method = "update",
                args = {MappedStatement.class, Object.class})
})
public class IdFillingInterceptor implements Interceptor {

    private static final String ID_GENERATOR_NAME = "idGenerator";

    private IdGenerator idGenerator;

    private static final Map<String, IdScopeController> ID_SCOPE_CONTROLLER_MAP = new ConcurrentHashMap<>();
    private static final Map<String, IdScope> PARAMETER_ID_SCOPE_MAP = new ConcurrentHashMap<>();

    @Override
    public Object intercept(Invocation invocation) throws Throwable {

        Object[] args = invocation.getArgs();
        MappedStatement ms = (MappedStatement) args[0];
        Object parameterObject = args[1];

        String msId = ms.getId();

        // IdScopeController可以压制注入ID
        IdScopeController idScopeController = extractIdScopeController(msId);

        if (idScopeController != null && IdScopeController.SUPPRESS == idScopeController.mode()) {
            return invocation.proceed();
        }

        if (idScopeController != null && IdScopeController.FORCE_INJECT != idScopeController.mode()) {
            throw new IdFillingException("cn.addenda.me.idfilling.annotation.IdScopeController.mode只有两种选项：FORCE_INJECT or SUPPRESS。");
        }

        if (parameterObject instanceof Collection) {
            injectCollection((Collection<?>) parameterObject, idScopeController);
        } else if (parameterObject instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) parameterObject;
            Set<? extends Map.Entry<?, ?>> entries = map.entrySet();
            Set<Integer> injectedHashCode = new HashSet<>();
            for (Map.Entry<?, ?> next : entries) {
                Object value = next.getValue();
                if (!injectedHashCode.contains(value.hashCode())) {
                    if (value instanceof Collection) {
                        injectCollection((Collection<?>) value, idScopeController);
                    } else {
                        injectPojo(value, idScopeController);
                    }
                    injectedHashCode.add(value.hashCode());
                }
            }
        } else {
            injectPojo(parameterObject, idScopeController);
        }
        return invocation.proceed();
    }

    private void injectCollection(Collection<?> collection, IdScopeController idScopeController) {
        for (Object parameter : collection) {
            injectPojo(parameter, idScopeController);
        }
    }

    private void injectPojo(Object object, IdScopeController idScopeController) {
        IdScope idScope = extractIdScopeFromObject(object);
        // 如果实体类上没有IdScope，不注入ID
        if (idScope == null) {
            return;
        }

        String scopeName = idScope.scopeName();
        if (scopeName == null) {
            throw new IdFillingException("cn.addenda.me.idfilling.annotation.IdScope注解的scopeName为必填项！");
        }

        boolean forceFlag = idScopeController != null && IdScopeController.FORCE_INJECT == idScopeController.mode();
        MetaObject metaObject = SystemMetaObject.forObject(object);

        if (forceFlag) {
            metaObject.setValue(idScope.idFieldName(), idGenerator.nextId(scopeName));
        } else {
            Object value = metaObject.getValue(idScope.idFieldName());
            if (value == null) {
                metaObject.setValue(idScope.idFieldName(), idGenerator.nextId(scopeName));
            }
        }
    }

    private IdScope extractIdScopeFromObject(Object object) {
        if (object == null) {
            return null;
        }
        Class<?> aClass = object.getClass();
        String className = aClass.getName();
        return PARAMETER_ID_SCOPE_MAP.computeIfAbsent
                (className, s -> MeAnnotationUtil.extractAnnotationFromClass(aClass, IdScope.class));
    }


    @Override
    public void setProperties(Properties properties) {
        if (properties.containsKey(ID_GENERATOR_NAME)) {
            String idGeneratorClassName = (String) properties.get(ID_GENERATOR_NAME);
            if (idGeneratorClassName != null) {
                idGenerator = newInstance(idGeneratorClassName);
            }
        } else {
            throw new IdFillingException("cn.addenda.me.idfilling.interceptor.IdFillingInterceptor初始化失败：拦截器需要[name=" + ID_GENERATOR_NAME + "]的property。");
        }
    }


    private IdGenerator newInstance(String clazzName) {
        try {
            Class<?> aClass = Class.forName(clazzName);
            if (!IdGenerator.class.isAssignableFrom(aClass)) {
                throw new IdFillingException("cn.addenda.me.idfilling.interceptor.IdFillingInterceptor初始化失败：[name=" + ID_GENERATOR_NAME + "]property的value需要是cn.addenda.me.idfilling.idgenerator.IdGenerator的子类。当前是：" + clazzName + "。");
            }

            // 如果IdGenerator存在单例方法，优先取单例方法。
            Method[] methods = aClass.getMethods();
            for (Method method : methods) {
                if (method.getName().equals("getInstance") && Modifier.isStatic(method.getModifiers()) &&
                        method.getParameterCount() == 0 && IdGenerator.class.isAssignableFrom(method.getReturnType())) {
                    return (IdGenerator) method.invoke(null);
                }
            }

            // 如果不存在单例方法，取默认构造函数
            return (IdGenerator) aClass.newInstance();
        } catch (Exception e) {
            throw new IdFillingException("cn.addenda.me.idfilling.interceptor.IdFillingInterceptor初始化失败：[name=" + ID_GENERATOR_NAME + "]property的value需要是cn.addenda.me.idfilling.idgenerator.IdGenerator的子类。当前是：" + clazzName + "。", e);
        }
    }

    private IdScopeController extractIdScopeController(String msId) {
        return ID_SCOPE_CONTROLLER_MAP.computeIfAbsent(msId,
                s -> {
                    int end = msId.lastIndexOf(".");
                    try {
                        Class<?> aClass = Class.forName(msId.substring(0, end));
                        String methodName = msId.substring(end + 1);
                        return MeAnnotationUtil.extractAnnotationFromMethod(aClass, methodName, IdScopeController.class);
                    } catch (ClassNotFoundException e) {
                        throw new IdFillingException("无法找到对应的Mapper：" + msId, e);
                    }
                });
    }


}
