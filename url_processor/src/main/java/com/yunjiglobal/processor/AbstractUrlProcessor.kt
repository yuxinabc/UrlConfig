package com.yunjiglobal.processor

import com.google.auto.common.MoreElements
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.yunjiglobal.annotations.UrlConfig
import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter
import java.io.Writer
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Filer
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeVariable
import javax.tools.Diagnostic

/**
 * 公司    云集共享科技
 * 创建时间 2019/2/13
 * 描述     urlConfig的注解处理
 * @author zhuxi
 */
internal abstract class AbstractUrlProcessor : AbstractProcessor() {
    private val stringType = "java.lang.String"
    private lateinit var filer: Filer

    override fun init(processingEnv: ProcessingEnvironment) {
        super.init(processingEnv)

        filer = processingEnv.filer
    }

    override fun getSupportedAnnotationTypes(): MutableSet<String> = linkedSetOf(UrlConfig::class.java.canonicalName)
    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latestSupported()
    override fun process(annotations: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment): Boolean {
        //获取到所有的带有注解的Element
        val annotatedWith = roundEnv.getElementsAnnotatedWith(UrlConfig::class.java)
        val buildMap = LinkedHashMap<TypeElement, FileBuild>()
        //循环便利，生成文件需要的java逻辑
        annotatedWith.forEach { element ->
            try {
                parseBindUrl(element, buildMap)
            } catch (e: Exception) {
                logParsingError(element, UrlConfig::class.java, e)
            }
        }
        for ((key, value) in buildMap) {
            //每一个FileBuilder都包含一个将要生成的java类
            val javaFile = value.buildJavaFile()
            try {
                //将代码逻辑生成文件
                javaFile.writeTo(filer)
            } catch (e: IOException) {
                error(key, "Unable to write binding for type %s: %s", key, e.message!!)
            }
        }
        return false
    }

    private fun logParsingError(element: Element, annotation: Class<out Annotation>, e: Exception) {
        val stackTrace = StringWriter() as Writer
        e.printStackTrace(PrintWriter(stackTrace))
        error(element, "Unable to parse @%s binding.\n\n%s", annotation.simpleName, stackTrace)
    }

    private fun error(element: Element, message: String, vararg args: Any) {
        printMessage(Diagnostic.Kind.ERROR, element, message, args)
    }

    private fun note(element: Element, message: String, vararg args: Any) {
        printMessage(Diagnostic.Kind.NOTE, element, message, args)
    }

    private fun printMessage(kind: Diagnostic.Kind, element: Element, message: String, args: Array<out Any>) {
        var message1 = message
        if (args.isNotEmpty()) {
            message1 = String.format(message, *args)
        }
        processingEnv.messager.printMessage(kind, message1, element)
    }

    private fun isInaccessibleViaGeneratedCode(annotationClass: Class<out Annotation>, targetThing: String, element: Element): Boolean {
        var hasError = false
        val enclosingElement = element.enclosingElement as TypeElement
        val modifiers = element.modifiers
        if (modifiers.contains(Modifier.PRIVATE) || !modifiers.contains(Modifier.STATIC)) {
            error(element, "@%s %s must not be private or nonstatic. (%s.%s)",
                    annotationClass.simpleName, targetThing, enclosingElement.qualifiedName, element.simpleName)
            hasError = true
        }
        if (enclosingElement.kind != ElementKind.CLASS) {
            error(enclosingElement, "@%s %s may only be contained in classes. (%s.%s)",
                    annotationClass.simpleName, targetThing, enclosingElement.qualifiedName, element.simpleName)
            hasError = true
        }
        if (enclosingElement.modifiers.contains(Modifier.PRIVATE)) {
            error(enclosingElement, "@%s %s may not be contained in private classes. (%s.%s)",
                    annotationClass.simpleName, targetThing, enclosingElement.qualifiedName, element.simpleName)
            hasError = true
        }
        return hasError
    }

    private fun isBindingInWrongPackage(annotationClass: Class<out Annotation>, element: Element): Boolean {
        val enclosingElement = element.enclosingElement as TypeElement
        val qualifiedName = enclosingElement.qualifiedName.toString()
        if (qualifiedName.startsWith("android.")) {
            error(element, "@%s-annotated class incorrectly in Android framework package. (%s)",
                    annotationClass.simpleName, qualifiedName)
            return true
        }
        if (qualifiedName.startsWith("java.")) {
            error(element, "@%s-annotated class incorrectly in Java framework package. (%s)",
                    annotationClass.simpleName, qualifiedName)
            return true
        }
        return false
    }

    @Suppress("UnstableApiUsage")
    private fun parseBindUrl(element: Element, map: MutableMap<TypeElement, FileBuild>) {
        val enclosingElement = element.enclosingElement as TypeElement
        var elementType = element.asType()
        var hasError = isInaccessibleViaGeneratedCode(UrlConfig::class.java, "fields", element)
                || isBindingInWrongPackage(UrlConfig::class.java, element)
        if (elementType.kind == TypeKind.TYPEVAR) {
            elementType = (elementType as TypeVariable).upperBound
        }
        val qualifiedName = enclosingElement.qualifiedName
        val simpleName = element.simpleName
        if (!Utils.isSubtypeOfType(elementType, stringType) && !Utils.isInterface(elementType)) {
            if (elementType.kind === TypeKind.ERROR) {
                note(element, "@%s field with unresolved type (%s) " + "must elsewhere be generated as a String or interface. (%s.%s)",
                        String::class.java.simpleName, elementType, qualifiedName, simpleName)
            } else {
                error(element, "@%s fields must extend from String or be an interface. (%s.%s)",
                        String::class.java.simpleName, qualifiedName, simpleName)
                hasError = true
            }
        }
        if (hasError) {
            return
        }
        val value = element.getAnnotation(UrlConfig::class.java).value
        var fileBuild = map[enclosingElement]
        if (fileBuild != null) {
            val existingBindingName = fileBuild.findExistingBindingName(value)
            if (existingBindingName != null) {
                error(element, "Attempt to use @%s for an already bound on '%s'. (%s.%s)",
                        UrlConfig::class.java.simpleName, existingBindingName,
                        enclosingElement.qualifiedName, element.simpleName)
                return
            }
        } else {
            val packageName = MoreElements.getPackage(enclosingElement).qualifiedName.toString()
            val className = enclosingElement.qualifiedName.toString().substring(
                    packageName.length + 1).replace('.', '$')
            val bindingClassName = ClassName.get(packageName, "${className}_Binding")
            var targetType = TypeName.get(enclosingElement.asType())
            if (targetType is ParameterizedTypeName) {
                targetType = targetType.rawType
            }
            fileBuild = FileBuild(bindingClassName, targetType)
            map[enclosingElement] = fileBuild
        }
        fileBuild.addField(simpleName.toString(), value)
    }
}