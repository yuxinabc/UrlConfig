package com.yunjiglobal.binder;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 公司    云集共享科技
 * 创建时间 2019/2/15
 * 描述     url自动化配置入口
 *
 * @author zhuxi
 */
public class UrlBinder {
    private static Map<Class<?>, Constructor<?>> BINDINGS = new LinkedHashMap<>();

    public static void bind(Class<?> clz, Map<String, Object> map) {
        Constructor<?> constructor = findBindingConstructorForClass(clz);
        if (constructor == null) {
            return;
        }
        try {
            constructor.newInstance(map);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Unable to invoke" + constructor, e);
        } catch (InstantiationException e) {
            throw new RuntimeException("Unable to invoke" + constructor, e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw new RuntimeException("Unable to create binding instance.", cause);
        }
    }

    private static Constructor<?> findBindingConstructorForClass(Class<?> clz) {
        Constructor<?> constructor = BINDINGS.get(clz);
        if (constructor != null && BINDINGS.containsKey(clz)) {
            return constructor;
        }
        String clsName = clz.getName();
        if (clsName.startsWith("android.") || clsName.startsWith("java.")
                || clsName.startsWith("androidx.")) {
            return null;
        }
        try {
            Class<?> bindingClass = clz.getClassLoader().loadClass(clsName + "_Binding");
            constructor = bindingClass.getConstructor(Map.class);
        } catch (ClassNotFoundException e) {
            constructor = findBindingConstructorForClass(clz.getSuperclass());
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Unable to find binding constructor for " + clsName, e);
        }
        if (constructor != null) {
            BINDINGS.put(clz, constructor);
        }
        return constructor;
    }
}
