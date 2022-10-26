package cz.encircled.skom

import java.math.BigDecimal
import kotlin.reflect.KClass

class MappingConfig(
    addBasicConverters: Boolean = true
) {

    internal var classToDescriptor: MutableMap<FromTo, MappingDescriptor> = mutableMapOf()
    internal val customMappers: MutableMap<FromTo, (Any) -> Map<String, Any?>> = mutableMapOf()
    internal val propertyAliases: MutableMap<FromTo, MutableMap<String, String>> = mutableMapOf()
    internal val directConverters: MutableMap<FromToJava, (Any) -> Any> = mutableMapOf()

    init {
        if (addBasicConverters) {
            addNumberConverters()
            addStringToNumberConverters()

            addConverter(Short::class, Boolean::class) { it == 1.toShort() }
        }
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

    fun <F : Any, T : Any> addMapping(from: KClass<F>, to: KClass<T>, mapper: (F) -> Map<String, Any?>) {
        customMappers[from to to] = mapper as (Any) -> Map<String, Any?>
    }

    fun <F : Any, T : Any> addConverter(from: KClass<F>, to: KClass<T>, mapper: (F) -> T) {
        directConverters[from to to.java] = mapper as (Any) -> Any
    }

    fun <F : Any, T : Any> addPropertyAlias(from: KClass<F>, to: KClass<T>, sourceName: String, targetName: String) {
        val fromTo = from to to
        propertyAliases.putIfAbsent(fromTo, mutableMapOf())
        propertyAliases.getValue(fromTo)[sourceName] = targetName
    }

    fun <F : Any, T : Any> builder(from: KClass<F>, to: KClass<T>): MappingConfigBuilder<F, T> {
        return MappingConfigBuilder(this, from, to)
    }

    class MappingConfigBuilder<F : Any, T : Any>(
        val config: MappingConfig,
        val from: KClass<F>,
        val to: KClass<T>,
    ) {
        fun addMapping(mapper: (F) -> Map<String, Any?>): MappingConfigBuilder<F, T> {
            config.addMapping(from, to, mapper)
            return this
        }

        fun addPropertyAlias(sourceName: String, targetName: String): MappingConfigBuilder<F, T> {
            config.addPropertyAlias(from, to, sourceName, targetName)
            return this
        }

        fun config(): MappingConfig = config

    }

}
