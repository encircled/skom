package cz.encircled.skom

import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

class TypeWrapper(val type: Type) {

    fun isParametrized(): Boolean {
        return type is ParameterizedType
    }

    fun typeArgument(i: Int): Type {
        return (type as ParameterizedType).actualTypeArguments[i]
    }

    fun rawClass(): Class<*> {
        val resultType = if (isParametrized()) (type as ParameterizedType).rawType else type
        return resultType as Class<*>
    }

    fun isEnum(): Boolean {
        return if (isParametrized()) false
        else (type as Class<*>).isEnum
    }

}