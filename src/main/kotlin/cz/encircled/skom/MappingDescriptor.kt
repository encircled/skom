package cz.encircled.skom

import java.lang.reflect.Modifier
import kotlin.reflect.*
import kotlin.reflect.KVisibility.PUBLIC
import kotlin.reflect.jvm.javaField

class KParamWrapper(val param: KParameter, val isMarkedNullable: Boolean, val type: KType, val isOptional: Boolean)

internal class MappingDescriptor<T>(
    val constructor: KFunction<T>,
    val sourceProperties: Collection<ObjectValueAccessor>,
    val targetProperties: Collection<ObjectValueAccessor>,

    val constructorParams: List<KParamWrapper> = constructor.parameters.map {
        KParamWrapper(
            it,
            it.type.isMarkedNullable,
            it.type,
            it.isOptional
        )
    }
)

class ObjectValueAccessor(
    private val callable: KCallable<*>
) {

    companion object {

        fun isValidGetAccessor(callable: KCallable<*>): Boolean {
            val visible = callable.visibility == PUBLIC
            return visible && (callable is KProperty<*> || callable.isGetterFunction())
        }

        fun isValidSetAccessor(callable: KCallable<*>): Boolean {
            val fieldOrSetter = callable is KMutableProperty<*> || callable.isSetterFunction()
            val visible = callable.visibility == PUBLIC
            return visible && fieldOrSetter && !callable.isStaticField()
        }

        private fun String.isBooleanGetter(): Boolean = startsWith("is") && get(2).isUpperCase()

        private fun KCallable<*>.isGetterFunction(): Boolean {
            val getterName = name.startsWith("get") && name[3].isUpperCase() || name.isBooleanGetter()

            // 0 for static callable and 1 for instance callable
            return getterName && parameters.size < 2
        }

        private fun KCallable<*>.isSetterFunction(): Boolean {
            val setterName = name.startsWith("set") && name[3].isUpperCase()
            return this is KFunction && setterName && parameters.size == 2
        }

        private fun KCallable<*>.isStaticField(): Boolean {
            return this is KProperty<*> && javaField != null && Modifier.isStatic(javaField!!.modifiers)
        }

    }

    private val isStatic = callable.isStaticField()
    private val isGetter = isValidGetAccessor(callable)
    private val isSetter = isValidSetAccessor(callable)
    private val isBooleanGetter = isGetter && callable.name.isBooleanGetter()

    val logicalName = getLogicalFieldName(callable)

    val returnType = if (callable.isSetterFunction()) callable.parameters[1].type else callable.returnType

    fun getValue(from: Any): Any? {
        return if (isStatic) callable.call() else callable.call(from)
    }

    fun setValue(target: Any, valueToSet: Any?) {
        if (!isSetter) {
            throw IllegalStateException("$logicalName is not a valid setter!")
        }

        if (callable is KMutableProperty<*>) {
            callable.setter.call(target, valueToSet)
        } else if (callable is KFunction<*>) {
            callable.call(target, valueToSet)
        }
    }

    /**
     * Get real field from getter/setter
     */
    private fun getLogicalFieldName(callable: KCallable<*>): String {
        val name = callable.name
        return when {
            callable !is KFunction -> name
            isBooleanGetter -> name.uncapitalize(2)
            isGetter || isSetter -> name.uncapitalize(3)
            else -> name
        }
    }

    private fun String.uncapitalize(from: Int): String {
        return substring(from).replaceFirstChar { it.lowercaseChar() }
    }

}