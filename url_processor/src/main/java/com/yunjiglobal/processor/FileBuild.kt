package com.yunjiglobal.processor

import com.squareup.javapoet.*
import javax.lang.model.element.Modifier

/**
 * 公司    云集共享科技
 * 创建时间 2019/2/13
 * 描述     java文件构建器
 * @author zhuxi
 */
internal class FileBuild(
        private val bindClassName: ClassName,
        private val targetTypeName: TypeName
) {
    private val mapTypeName = ClassName.get("java.util", "Map")
    private val stringTypeName = ClassName.get("java.lang", "String")
    private val objectTypeName = ClassName.get("java.lang", "Object")
    private val textUtilsTypeName = ClassName.get("android.text", "TextUtils")
    private val fieldMap = LinkedHashMap<String, FieldHolder>()
    fun buildJavaFile(): JavaFile {
        val bindingConfiguration = buildTypeSpec()
        return JavaFile.builder(bindClassName.packageName(), bindingConfiguration)
                .addFileComment("Generated code from UrlProcessor. Do not modify!")
                .build()
    }

    fun addField(name: String, value: String) {
        fieldMap[value] = FieldHolder(name, value)
    }

    fun findExistingBindingName(value: String) = fieldMap[value]?.name

    private fun buildTypeSpec(): TypeSpec {
        val classBuilder = TypeSpec.classBuilder(bindClassName.simpleName()).addModifiers(Modifier.PUBLIC)
        val mapParameterizedName = ParameterizedTypeName.get(mapTypeName, stringTypeName, objectTypeName)
        val methodSpec = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(mapParameterizedName, "map")
        fieldMap.values.forEach {
            val codeBuilder = CodeBlock.builder()
            if (it.value.contains("/")) {
                val splitList = it.value.split("/")
                var flowSize = 0
                for (i in 0 until splitList.size) {
                    if ("" == splitList[i]) {
                        break
                    }
                    when (i) {
                        0 -> {
                            codeBuilder.beginControlFlow("if(map.get(\$S) instanceof \$T)", splitList[i], mapTypeName)
                            codeBuilder.addStatement("\$T innerMap = (\$T) map.get(\$S)", mapParameterizedName, mapParameterizedName, splitList[i])
                            flowSize++
                        }
                        splitList.lastIndex -> {
                            codeBuilder.beginControlFlow("if(innerMap.get(\$S) instanceof \$T)", splitList[i], stringTypeName)
                            codeBuilder.addStatement("\$T value = (\$T) innerMap.get(\$S)", stringTypeName, stringTypeName, splitList[i])
                            flowSize++
                            codeBuilder.beginControlFlow("if(!\$T.isEmpty(value))", textUtilsTypeName)
                            codeBuilder.addStatement("\$T.\$N = value", targetTypeName, it.name)
                            flowSize++
                        }
                        else -> {
                            codeBuilder.beginControlFlow("if(innerMap.get(\$S) instanceof \$T)", splitList[i], mapTypeName)
                            codeBuilder.addStatement("innerMap = (\$T) innerMap.get(\$S)", mapParameterizedName, splitList[i])
                            flowSize++
                        }
                    }
                }
                for (i in 1..flowSize) {
                    codeBuilder.endControlFlow()
                }
            } else {
                codeBuilder.beginControlFlow("if(map.get(\$S) instanceof \$T)", it.value, stringTypeName)
                codeBuilder.addStatement("\$T value = (\$T) map.get(\$S)", stringTypeName, stringTypeName, it.value)
                codeBuilder.beginControlFlow("if(!\$T.isEmpty(value))", textUtilsTypeName)
                codeBuilder.addStatement("\$T.\$N = value", targetTypeName, it.name)
                codeBuilder.endControlFlow()
                codeBuilder.endControlFlow()
            }
            methodSpec.addCode(codeBuilder.build())
//            methodSpec.addStatement("\$L", codeBuilder.build())
        }
        classBuilder.addMethod(methodSpec.build())
        return classBuilder.build()
    }

    private data class FieldHolder(val name: String, val value: String)
}
