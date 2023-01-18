package cz.encircled.skom

import java.math.BigDecimal
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

class MappingConfig(
    addBasicConverters: Boolean = true
) {

    internal var classToDescriptor: MutableMap<FromTo, MappingDescriptor<*>> = ConcurrentHashMap()
    internal val enumMappers: MutableMap<FromToJava, MutableMap<Any, Any>> = mutableMapOf()
    internal val customMappers: MutableMap<FromTo, (Any) -> Map<String, Any?>> = mutableMapOf()
    private val propertyAliases: MutableMap<FromTo, MutableMap<String, MutableSet<String>>> = mutableMapOf()
    internal val directConverters: MutableMap<FromToJava, (Any) -> Any> = mutableMapOf()

    init {
        if (addBasicConverters) {
            addNumberConverters()
            addStringToNumberConverters()

            addConverter(Short::class, Boolean::class) { it == 1.toShort() }
        }
    }

    fun <F : Any, T : Any> forClasses(
        from: KClass<F>,
        to: KClass<T>,
        init: MappingConfigBuilder<F, T>.() -> Unit
    ): MappingConfig {
        val forClasses = MappingConfigBuilder(this, from, to)
        forClasses.init()
        return this
    }

    fun <F : Any, T : Any> addConverter(from: KClass<F>, to: KClass<T>, mapper: (F) -> T) {
        directConverters[from to to.java] = mapper as (Any) -> Any
    }

    private fun addStringToNumberConverters() {
        addConverter(String::class, BigDecimal::class) { it.toBigDecimal() }
        addConverter(String::class, Int::class) { it.toInt() }
        addConverter(String::class, Long::class) { it.toLong() }
        addConverter(String::class, Float::class) { it.toFloat() }
    }

    private fun addNumberConverters() {
        addConverter(Float::class, BigDecimal::class) { it.toBigDecimal() }
        addConverter(Float::class, Int::class) { it.toInt() }
        addConverter(Float::class, Long::class) { it.toLong() }

        addConverter(BigDecimal::class, Float::class) { it.toFloat() }
        addConverter(BigDecimal::class, Long::class) { it.toLong() }

        addConverter(Long::class, BigDecimal::class) { it.toBigDecimal() }
        addConverter(Long::class, Int::class) { it.toInt() }
        addConverter(Long::class, Float::class) { it.toFloat() }

        addConverter(Int::class, BigDecimal::class) { it.toBigDecimal() }
        addConverter(Int::class, Long::class) { it.toLong() }
        addConverter(Int::class, Float::class) { it.toFloat() }

        addConverter(BigDecimal::class, Int::class) { it.toInt() }
    }

    internal fun <F : Any, T : Any> addMapping(from: KClass<F>, to: KClass<T>, mapper: (F) -> Map<String, Any?>) {
        customMappers[from to to] = mapper as (Any) -> Map<String, Any?>
    }

    internal fun <F : Any, T : Any> addEnumMapping(from: KClass<F>, to: KClass<T>, left: Enum<*>, right: Enum<*>) {
        val leftToRight = enumMappers.computeIfAbsent(from to to.java) { hashMapOf() }
        leftToRight[left] = right
        val rightToLeft = enumMappers.computeIfAbsent(to to from.java) { hashMapOf() }
        rightToLeft[right] = left
    }

    internal fun <F : Any, T : Any> addPropertyAlias(
        from: KClass<F>,
        to: KClass<T>,
        sourceName: String,
        targetName: String
    ) {
        val fromTo = from to to
        val aliasesForPair = propertyAliases.computeIfAbsent(fromTo) { hashMapOf() }
        aliasesForPair.computeIfAbsent(sourceName) { mutableSetOf() }
        aliasesForPair.getValue(sourceName).add(targetName)
    }

    internal fun directConverter(value: Any, target: TypeWrapper): ((Any) -> Any)? {
        return directConverters[value::class to target.type]
    }

    internal fun enumMapper(value: Any, target: TypeWrapper): MutableMap<Any, Any>? {
        return enumMappers[value::class to target.type]
    }

    internal fun aliasesForProperty(fromTo: FromTo, propertyName: String): Set<String> {
        return propertyAliases[fromTo]?.get(propertyName) ?: setOf()
    }

    class MappingConfigBuilder<F : Any, T : Any>(
        val config: MappingConfig,
        val from: KClass<F>,
        val to: KClass<T>,
    ) {
        fun addPropertyMappings(mapper: (F) -> Map<String, Any?>): MappingConfigBuilder<F, T> {
            config.addMapping(from, to, mapper)
            return this
        }

        fun addPropertyAlias(sourceName: String, vararg targetNames: String): MappingConfigBuilder<F, T> {
            targetNames.forEach {
                config.addPropertyAlias(from, to, sourceName, it)
            }
            return this
        }

    }

}
