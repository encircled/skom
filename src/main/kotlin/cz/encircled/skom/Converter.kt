package cz.encircled.skom

import java.lang.reflect.Type
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.jvm.javaType

internal class Converter(
    private val config: MappingConfig,
    private val mapper: SimpleKotlinObjectMapper
) {

    internal fun <T : Any> isDirectlyConvertable(value: T, targetType: KClass<*>): Boolean {
        val target = TypeWrapper(targetType.java)
        return config.directConverter(value, target) != null || (value is Enum<*> && target.isEnum())
    }

    internal fun <T : Any> convertValue(value: T?, target: KType): Any? {
        if (value == null) return null
        val javaType = target.javaType
        if (value::class.java == javaType) return value
        return convertValue(value, javaType)
    }

    internal fun <T : Any> convertValue(value: T?, targetType: Type): Any? {
        if (value == null || value::class.java == targetType) return value
        val target = TypeWrapper(targetType)

        // Check for kotlin vs java primitive classes, like kotlin.Long == java.lang.Long
        if (value::class == target.rawClass().kotlin) {
            return value
        }

        return when (value) {
            is Convertable -> mapper.mapTo(value, target.rawClass().kotlin)
            is Collection<*> -> convertCollection(target, value)
            is Map<*, *> -> convertMap(target, value)
            else -> convertSingularObject(target, value)
        }
    }

    private fun <T : Any> convertSingularObject(target: TypeWrapper, value: T): Any? {
        val directConverter = config.directConverter(value, target)
        return when {
            directConverter != null -> directConverter.invoke(value)
            value is Enum<*> && target.isEnum() -> convertEnum(target, value)
            target.type == String::class.java -> value.toString()
            else -> {
                if (target.rawClass().isAssignableFrom(value.javaClass)) {
                    value
                } else throw IllegalStateException("No converter provided for mapping ${value::class} to ${target.type}")
            }
        }
    }

    private fun convertCollection(target: TypeWrapper, value: Collection<*>): Collection<Any?> {
        val type = target.typeArgument(0)
        val listResult = value.map { v -> convertValue(v, type) }
        return when (target.rawClass()) {
            Set::class.java -> listResult.toMutableSet()
            else -> listResult.toMutableList()
        }
    }

    private fun convertMap(target: TypeWrapper, value: Map<*, *>): Map<Any?, Any?> {
        val keyType = target.typeArgument(0)
        val valueType = target.typeArgument(1)

        return value
            .mapKeys { convertValue(it.key, keyType) }
            .mapValues { convertValue(it.value, valueType) }
    }

    private fun convertEnum(target: TypeWrapper, value: Any): Any? {
        val mapper = config.enumMapper(value, target)
        return if (mapper != null && mapper.containsKey(value)) {
            mapper[value]
        } else {
            java.lang.Enum.valueOf(
                target.type as Class<out Enum<*>>,
                (value as Enum<*>).name
            )
        }
    }

}