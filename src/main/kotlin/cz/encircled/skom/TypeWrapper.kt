package cz.encircled.skom

import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.TypeVariable
import java.lang.reflect.WildcardType

class TypeWrapper(val type: Type) {

    fun isParametrized(): Boolean {
        return type is ParameterizedType || type is WildcardType
    }

    fun typeArgument(i: Int): Type {
        return (type as ParameterizedType).actualTypeArguments[i]
    }

    fun rawClass(): Class<*> {
        val resultType = when (type) {
            is ParameterizedType -> type.rawType
            is WildcardType -> type.upperBounds?.firstOrNull() ?: type.lowerBounds?.firstOrNull()
            is TypeVariable<*> -> type.bounds[0]
            else -> type
        }

        return resultType as Class<*>
    }

    fun isEnum(): Boolean {
        return if (isParametrized()) false
        else (type as Class<*>).isEnum
    }

}