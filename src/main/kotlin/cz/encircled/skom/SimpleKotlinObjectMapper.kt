package cz.encircled.skom

import java.util.IdentityHashMap
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

    fun <T : Any> mapManyTo(classTo: KClass<T>, vararg many: Any): T {
        if (many.isEmpty()) {
            throw IllegalArgumentException("At least one source object must be provided")
        }
        val visited = IdentityHashMap<Any, Any?>()
        val sourceNameToValue: MutableMap<String, Any?> = mutableMapOf()
        var descriptor: MappingDescriptor<T>? = null

        many.forEach { from ->
            val fromTo = Pair(from::class, classTo)
            descriptor = getClassDescriptor(fromTo, from)

            sourceNameToValue.mergeAll(getValuesFromSource(descriptor!!, fromTo, from))
            sourceNameToValue.mergeAll(config.customMappers[fromTo]?.mapProperties(from))
        }

        return constructFromSources(many, descriptor!!, sourceNameToValue, visited)
    }

    fun <T : Any> mapTo(from: Any, classTo: KClass<T>): T {
        return mapToInternal(from, classTo, IdentityHashMap<Any, Any?>())
    }

    internal fun <T : Any> mapToInternal(from: Any, classTo: KClass<T>, visited: MutableMap<Any, Any?>): T {
        if (from::class == classTo) {
            return from as T
        }

        if (visited.containsKey(from)) {
            return visited[from] as T
        }
        visited[from] = from

        if (converter.isDirectlyConvertable(from, classTo)) {
            return converter.convertValue(from, classTo.java, visited) as T
        }

        val fromTo = Pair(from::class, classTo)
        val descriptor = getClassDescriptor<T>(fromTo, from)

        val sourceNameToValue: MutableMap<String, Any?> = getValuesFromSource(descriptor, fromTo, from)
        sourceNameToValue.putAll(config.customMappers[fromTo]?.mapProperties(from) ?: mapOf())

        return constructFromSources(from, descriptor, sourceNameToValue, visited)
    }

    fun config() = config

    private fun <T : Any> constructFromSources(
        from: Any,
        descriptor: MappingDescriptor<T>,
        sourceNameToValue: MutableMap<String, Any?>,
        visited: MutableMap<Any, Any?>
    ): T {
        val targetConstParams: MutableMap<KParameter, Any?> =
            buildArgsForConstructor(descriptor, sourceNameToValue, visited)
        val targetObject = descriptor.constructor.callBy(targetConstParams)
        visited[from] = targetObject

        descriptor.targetProperties.forEach {
            val v = sourceNameToValue[it.logicalName]
            if (v != null) {
                it.setValue(targetObject, converter.convertValue(v, it.returnType, visited))
            }
        }

        return targetObject
    }

    private fun <T> buildArgsForConstructor(
        descriptor: MappingDescriptor<T>,
        sourceNameToValue: MutableMap<String, Any?>,
        visited: MutableMap<Any, Any?>
    ): MutableMap<KParameter, Any?> {
        return descriptor.constructorParams.fold(mutableMapOf()) { result, it ->
            val hasNoDefaultValue = !it.isOptional
            val value = converter.convertValue(sourceNameToValue[it.param.name], it.type, visited)
            if (value != null || (it.isMarkedNullable && hasNoDefaultValue)) {
                result[it.param] = value
            } else if (hasNoDefaultValue) {
                throw IllegalStateException("Mandatory constructor arg [${it.param.name}] is null!")
            }
            result
        }
    }

    private fun <T : Any> getValuesFromSource(
        descriptor: MappingDescriptor<T>, fromTo: Pair<KClass<out Any>, KClass<T>>, from: Any
    ): MutableMap<String, Any?> {
        return descriptor.sourceProperties.fold(mutableMapOf()) { result, property ->
            val value = property.getValue(from)
            result[property.logicalName] = value
            config.aliasesForProperty(fromTo, property.logicalName).forEach { alias ->
                result[alias] = value
            }
            result
        }
    }

    internal fun <T : Any> getClassDescriptor(fromTo: FromTo, from: Any): MappingDescriptor<T> {
        val descriptor = config.classToDescriptor.computeIfAbsent(fromTo) {
            val targetProperties = fromTo.second.members
                .filter { ObjectValueAccessor.isValidSetAccessor(it) }
                .map { ObjectValueAccessor(it) }

            val sourceProperties = from::class.members
                .filter { ObjectValueAccessor.isValidGetAccessor(it) }
                .map { ObjectValueAccessor(it) }

            MappingDescriptor(
                fromTo.second.constructors.first(),
                sourceProperties,
                targetProperties,
            )
        }

        return descriptor as MappingDescriptor<T>
    }

    private fun <K : Any, V> MutableMap<K, V>.mergeAll(another: Map<K, V>?) {
        another?.forEach { (k, v) ->
            if (v != null) {
                merge(k, v) { _, newValue -> newValue }
            }
        }
    }

}
