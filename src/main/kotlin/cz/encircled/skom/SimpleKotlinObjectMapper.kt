package cz.encircled.skom

import kotlin.reflect.KClass
import kotlin.reflect.KParameter

typealias FromTo = Pair<KClass<*>, KClass<*>>
typealias FromToJava = Pair<KClass<*>, Class<*>>

class SimpleKotlinObjectMapper(init: MappingConfig.() -> Unit) {

    internal val config: MappingConfig = MappingConfig()
    internal val converter: Converter = Converter(config, this)

    init {
        init(config)
    }

    fun <T : Any> mapTo(from: Any, classTo: KClass<T>): T {
        if (from::class == classTo) {
            return from as T
        }

        if (converter.isDirectlyConvertable(from, classTo)) {
            return converter.convertValue(from, classTo.java) as T
        }

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
            prop?.setValue(targetObject, converter.convertValue(value, prop.returnType))
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
            val value = it.getValue(from)
            result[it.logicalName] = value
            config.aliasesForProperty(fromTo, it.logicalName).forEach { alias ->
                result[alias] = value
            }
        }

        return result
    }

    internal fun <T : Any> getClassDescriptor(fromTo: FromTo, from: Any): MappingDescriptor<T> {
        val descriptor = config.classToDescriptor.computeIfAbsent(fromTo) {
            val targetProperties = fromTo.second.members
                .filter { ObjectValueAccessor.isValidSetAccessor(it) }
                .map { ObjectValueAccessor(it) }

            val sourceProperties = from::class.members
                .filter { ObjectValueAccessor.isValidGetAccessor(it) }
                .map { ObjectValueAccessor(it) }

            MappingDescriptor(fromTo.second.constructors.first(),
                sourceProperties,
                targetProperties,
                targetProperties.associateBy {
                    it.logicalName
                })
        }

        return descriptor as MappingDescriptor<T>
    }


}
