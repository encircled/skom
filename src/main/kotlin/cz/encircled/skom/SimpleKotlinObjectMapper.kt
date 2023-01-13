package cz.encircled.skom

import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KParameter
import kotlin.reflect.full.memberProperties

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
            !descriptor.targetConstructorParamNames.contains(it.key) &&
                    descriptor.targetPropertiesByName.containsKey(it.key)
        }

        postConstructValues.forEach { (name, value) ->
            val prop = descriptor.targetPropertiesByName[name]
            if (prop is KMutableProperty<*>) {
                prop.setter.call(targetObject, converter.convertValue(value, prop.returnType))
            }
        }

        return targetObject
    }

    fun config() = config

    private fun <T> buildArgsForConstructor(
        descriptor: MappingDescriptor<T>,
        sourceNameToValue: MutableMap<String, Any?>
    ): MutableMap<KParameter, Any?> {
        val targetConstructorParams: MutableMap<KParameter, Any?> = HashMap(descriptor.constructor.parameters.size)
        descriptor.constructor.parameters.forEach {
            val isNotOptional = !it.isOptional

            val value = converter.convertValue(sourceNameToValue[it.name], it.type)
            if (value != null || (it.type.isMarkedNullable && isNotOptional)) {
                targetConstructorParams[it] = value
            } else if (isNotOptional) {
                throw IllegalStateException("Mandatory constructor arg [${it.name}] is null!")
            }
        }
        return targetConstructorParams
    }

    private fun <T : Any> getValuesFromSource(
        descriptor: MappingDescriptor<T>,
        fromTo: Pair<KClass<out Any>, KClass<T>>,
        from: Any
    ): MutableMap<String, Any?> {
        return descriptor.sourceProperties.associate {
            val name = config.propertyAliases[fromTo]?.get(it.name) ?: it.name
            name to it.call(from)
        }.toMutableMap()
    }

    private fun <T : Any> getClassDescriptor(fromTo: FromTo, from: Any): MappingDescriptor<T> {
        val descriptor = config.classToDescriptor.computeIfAbsent(fromTo) {
            MappingDescriptor(
                fromTo.second.constructors.first(),
                from::class.memberProperties,
                fromTo.second.memberProperties.filterIsInstance<KMutableProperty<*>>()
            )
        }
        return descriptor as MappingDescriptor<T>
    }

}
