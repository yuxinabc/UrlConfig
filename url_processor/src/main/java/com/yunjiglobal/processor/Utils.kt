package com.yunjiglobal.processor

import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror

/**
 * 公司    云集共享科技
 * 创建时间 2019/2/13
 * 描述     TODO
 * @author zhuxi
 */
internal class Utils {
    companion object {
        fun isInterface(typeMirror: TypeMirror) = typeMirror is DeclaredType && typeMirror.asElement().kind == ElementKind.INTERFACE

        fun isSubtypeOfType(typeMirror: TypeMirror, otherType: String): Boolean {
            if (otherType == typeMirror.toString()) {
                return true
            }
            if (typeMirror.kind != TypeKind.DECLARED) {
                return false
            }
            val declaredType = typeMirror as DeclaredType
            val typeArguments = declaredType.typeArguments
            if (typeArguments.isNotEmpty()) {
                val string = buildString {
                    append(declaredType.asElement().toString())
                    append('<')
                    for (i in 0 until typeArguments.size) {
                        if (i > 0) {
                            append(",")
                        }
                        append('?')
                    }
                    append('>')
                }
                if (string == otherType) {
                    return true
                }
            }
            val element = declaredType.asElement() as? TypeElement ?: return false
            if (isSubtypeOfType(element.superclass, otherType)) {
                return true
            }
            element.interfaces.forEach {
                if (isSubtypeOfType(it, otherType)) {
                    return true
                }
            }
            return false
        }
    }
}