package cz.encircled.skom

import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import kotlin.reflect.*
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaType

typealias FromTo = Pair<KClass<*>, KClass<*>>
typealias FromToJava = Pair<KClass<*>, Class<*>>

open class SimpleKotlinObjectMapper(
    val config: MappingConfig = MappingConfig()
) {

    constructor(init: MappingConfig.() -> Unit) : this(
        MappingConfig()
    ) {
        init(config)
    }

    // TODO when classTo is primitive
    fun <T : Any> mapTo(from: Any, classTo: KClass<T>): T {
        val fromTo = Pair(from::class, classTo)
        val descriptor = config.classToDescriptor.computeIfAbsent(fromTo) {
            MappingDescriptor(
                classTo.constructors.first(),
                from::class.memberProperties,
                classTo.memberProperties.filterIsInstance<KMutableProperty<*>>()
            )
        }

        val sourceNameToValue: MutableMap<String, Any?> = HashMap(descriptor.sourceProperties.size)
        descriptor.sourceProperties.forEach {
            val name = config.propertyAliases[fromTo]?.get(it.name) ?: it.name
            val value = it.call(from)
            sourceNameToValue[name] = value
        }

        sourceNameToValue.putAll(config.customMappers[fromTo]?.invoke(from) ?: mapOf())
        val targetConstructorParams: MutableMap<KParameter, Any?> = HashMap(descriptor.constructor.parameters.size)
        descriptor.constructor.parameters.forEach {
            val name = it.name
            val isNotOptional = !it.isOptional

            val value = convertValue(sourceNameToValue[name], it.type)
            if (value != null || (it.type.isMarkedNullable && isNotOptional)) {
                targetConstructorParams[it] = value
            } else if (isNotOptional) {
                throw IllegalStateException("Mandatory constructor arg [$name] is null!")
            }
        }
        val target = descriptor.constructor.callBy(targetConstructorParams)

        sourceNameToValue
            .filter {
                !descriptor.targetConstructorParamNames.contains(it.key) &&
                        descriptor.targetPropertiesByName.containsKey(it.key)
            }
            .forEach { (name, value) ->
                val prop = descriptor.targetPropertiesByName[name]
                if (prop is KMutableProperty<*>) {
                    prop.setter.call(target, convertValue(value, prop.returnType))
                }
            }

        return target as T
    }

    internal fun <T : Any> convertValue(value: T?, target: Type): Any? {
        if (value == null || value::class.java == target) return value

        return when (value) {
            is Collection<*> -> {
                val type = (target as ParameterizedType).actualTypeArguments[0]
                value.map { v -> convertValue(v, type) }
            }
            is Convertable -> {
                mapTo(value, (target as Class<*>).kotlin)
            }
            is Map<*, *> -> {
                val keyType = (target as ParameterizedType).actualTypeArguments[0]
                val valueType = target.actualTypeArguments[1]

                value.mapKeys { convertValue(it.key, keyType) }
                    .mapValues { convertValue(it.value, valueType) }
            }
            else -> {
                if (target is Class<*>) {
                    val plainConverter = config.directConverters[value::class to target]
                    when {
                        plainConverter != null -> plainConverter.invoke(value)
                        target == String::class.java -> value.toString()
                        else -> value
                    }
                } else {
                    // TODO support generic types as well
                    value
                }
            }
        }
    }

    internal fun <T : Any> convertValue(value: T?, target: KType): Any? {
        if (value == null) return null
        val javaType = target.javaType
        if (value::class.java == javaType) return value
        return convertValue(value, javaType)
    }

}
