package com.yunjiglobal.processor;

import com.google.auto.common.MoreElements;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.yunjiglobal.annotations.UrlConfig;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

@AutoService(Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_7)
@SupportedAnnotationTypes("com.yunjiglobal.annotations.UrlConfig")
public class JavaUrlProcessor extends AbstractProcessor {
    private Elements elementUtils;
    private Types typeUtils;
    private Filer filerUtils;
    private Messager messager;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        elementUtils = processingEnvironment.getElementUtils();
        typeUtils = processingEnvironment.getTypeUtils();
        filerUtils = processingEnvironment.getFiler();
        messager = processingEnvironment.getMessager();
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        //获得所有被UrlConfig注解的属性集合
        Set<? extends Element> routeElements = roundEnvironment.getElementsAnnotatedWith(UrlConfig.class);
        Iterator<? extends Element> iterator = routeElements.iterator();
        //创建参数类型
        ParameterizedTypeName parameterizedTypeName = ParameterizedTypeName.get(Map.class, String.class, String.class);
        //创建参数
        ParameterSpec parameterSpec = ParameterSpec.builder(parameterizedTypeName, "map").build();
        //创建方法
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("loadData").addModifiers(Modifier.STATIC).addParameter(parameterSpec);
        TypeElement enclosingElement = null;
        while (iterator.hasNext()) {
            //因为注解作用在属性上，强转成VariableElement
            VariableElement variableElement = (VariableElement) iterator.next();
            if (variableElement.getEnclosingElement() instanceof TypeElement) {
                enclosingElement = (TypeElement) variableElement.getEnclosingElement();
            }
            UrlConfig annotation = variableElement.getAnnotation(UrlConfig.class);
            String key = annotation.value();
            String filedName = variableElement.getSimpleName().toString();
            methodBuilder.addStatement("IBaseUrl.$L=map.get($S)", filedName, key);
        }
        if(enclosingElement!=null){
            String packageName = MoreElements.getPackage(enclosingElement).getQualifiedName().toString();
            //创建类
            String className = enclosingElement.getQualifiedName().toString().substring(
                    packageName.length() + 1).replace('.', '$');
            ClassName bindingClassName = ClassName.get(packageName, className + "_Binding");
            TypeSpec typeSpec = TypeSpec.classBuilder(bindingClassName).addMethod(methodBuilder.build()).build();
            //创建java文件
            JavaFile javaFile = JavaFile.builder(packageName, typeSpec).build();
            try {
                javaFile.writeTo(filerUtils);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }
}
