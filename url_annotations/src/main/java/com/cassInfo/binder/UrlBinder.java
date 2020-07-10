package com.cassInfo.binder;

import java.lang.reflect.Method;
import java.util.Map;

public class UrlBinder {
    public static void bind(Class clz, Map<String,String> map){
        String name = clz.getName();
        ClassLoader classLoader = clz.getClassLoader();
        try {
            Class<?> aClass = classLoader.loadClass(name + "_Binding");
            Method loadData = aClass.getDeclaredMethod("loadData",Map.class);
            loadData.setAccessible(true);
            loadData.invoke(null,map);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
