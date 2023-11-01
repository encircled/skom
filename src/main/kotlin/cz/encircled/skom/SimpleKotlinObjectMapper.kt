package cz.encircled.skom

import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KParameter
import kotlin.reflect.KVisibility.PUBLIC
import kotlin.reflect.full.declaredMembers

typealias FromTo = Pair<KClass<*>, KClass<*>>
typealias FromToJava = Pair<KClass<*>, Class<*>>

class SimpleKotlinObjectMapper(init: MappingConfig.() -> Unit) {

    internal val config: MappingConfig = MappingConfig()
    internal val converter: Converter = Converter(config, this)

    init {
        init(config)
    }

    // TODO when classTo is primitive
    fun <T : Any> mapTo(from: Any, classTo: KClass<T>): T {
        val fromTo = Pair(from::class, classTo)
        val descriptor = getClassDescriptor<T>(fromTo, from)

        val sourceNameToValue: MutableMap<String, Any?> = getValuesFromSource(descriptor, fromTo, from)
        sourceNameToValue.putAll(config.customMappers[fromTo]?.invoke(from) ?: mapOf())

        val targetConstParams: MutableMap<KParameter, Any?> = buildArgsForConstructor(descriptor, sourceNameToValue)
        val targetObject = descriptor.constructor.callBy(targetConstParams)

        val postConstructValues = sourceNameToValue.filter {
            !descriptor.targetConstructorParamNames.contains(it.key) && descriptor.targetPropertiesByName.containsKey(it.key)
        }

        postConstructValues.forEach { (name, value) ->
            val prop = descriptor.targetPropertiesByName[name]
            if (prop is KMutableProperty<*>) {
                prop.setter.call(targetObject, converter.convertValue(value, prop.returnType))
            } else if (prop is KFunction<*>) {
                prop.call(targetObject, converter.convertValue(value, prop.returnType))
            }
        }

        return targetObject
    }

    fun config() = config

    private fun <T> buildArgsForConstructor(
        descriptor: MappingDescriptor<T>, sourceNameToValue: MutableMap<String, Any?>
    ): MutableMap<KParameter, Any?> {
        val targetConstructorParams: MutableMap<KParameter, Any?> = HashMap(descriptor.constructor.parameters.size)
        descriptor.constructor.parameters.forEach {
            val hasNoDefaultValue = !it.isOptional

            val value = converter.convertValue(sourceNameToValue[it.name], it.type)
            if (value != null || (it.type.isMarkedNullable && hasNoDefaultValue)) {
                targetConstructorParams[it] = value
            } else if (hasNoDefaultValue) {
                throw IllegalStateException("Mandatory constructor arg [${it.name}] is null!")
            }
        }
        return targetConstructorParams
    }

    private fun <T : Any> getValuesFromSource(
        descriptor: MappingDescriptor<T>, fromTo: Pair<KClass<out Any>, KClass<T>>, from: Any
    ): MutableMap<String, Any?> {
        val result = mutableMapOf<String, Any?>()

        descriptor.sourceProperties.forEach {
            val value = it.call(from)
            val name = getFieldName(it.name)

            result[name] = value
            config.aliasesForProperty(fromTo, name).forEach { alias ->
                result[alias] = value
            }
        }

        return result
    }

    internal fun <T : Any> getClassDescriptor(fromTo: FromTo, from: Any): MappingDescriptor<T> {
        val descriptor = config.classToDescriptor.computeIfAbsent(fromTo) {
            val targetProperties = fromTo.second.declaredMembers.filter {
                it.visibility == PUBLIC && (it is KMutableProperty<*> || (it is KFunction && it.name.isSetter()))
            }

            val sourceProperties = from::class.declaredMembers.filter {
                it.visibility == PUBLIC && (it !is KFunction || it.name.isGetter())
            }

            MappingDescriptor(fromTo.second.constructors.first(),
                sourceProperties,
                targetProperties,
                targetProperties.associateBy { getFieldName(it.name) })
        }

        return descriptor as MappingDescriptor<T>
    }

    /**
     * Get real field from getter/setter
     */
    internal fun getFieldName(accessorName: String): String {
        return if (accessorName.isBooleanGetter()) {
            accessorName.substring(2).replaceFirstChar { it.lowercaseChar() }
        } else if (accessorName.isGetter() || accessorName.isSetter()) {
            accessorName.substring(3).replaceFirstChar { it.lowercaseChar() }
        } else {
            accessorName
        }
    }

    private fun String.isBooleanGetter(): Boolean = startsWith("is") && get(2).isUpperCase()

    private fun String.isGetter(): Boolean = startsWith("get") && get(3).isUpperCase() || isBooleanGetter()

    private fun String.isSetter(): Boolean = startsWith("set") && get(3).isUpperCase()

}
