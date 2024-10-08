package cz.encircled.skom

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1

class MappingConfig(
    addBasicConverters: Boolean = true
) {

    internal var classToDescriptor: MutableMap<FromTo, MappingDescriptor<*>> = ConcurrentHashMap()
    internal val enumMappers: MutableMap<FromToJava, MutableMap<Any, Any>> = mutableMapOf()
    val customMappers: MutableMap<FromTo, CustomPropertyMapper> = mutableMapOf()
    private val propertyAliases: MutableMap<FromTo, MutableMap<String, MutableSet<String>>> = mutableMapOf()
    internal val directConverters: MutableMap<FromToJava, (Any) -> Any> = mutableMapOf()
    internal val boxedClasses = mapOf(
        Int::class.java to Integer::class.java,
        Long::class.java to java.lang.Long::class.java,
        Float::class.java to java.lang.Float::class.java,
        Double::class.java to java.lang.Double::class.java,
        Boolean::class.java to java.lang.Boolean::class.java,
    )

    init {
        if (addBasicConverters) {
            addNumberConverters()
            addStringToNumberConverters()
            addDatesConverters()

            addConverter(String::class, Boolean::class) { it == "true" }
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
        boxedClasses[to.java]?.let {
            directConverters[from to it] = mapper as (Any) -> Any
        }
    }

    private fun addStringToNumberConverters() {
        addConverter(String::class, BigDecimal::class) { it.toBigDecimal() }
        addConverter(String::class, Int::class) { it.toInt() }
        addConverter(String::class, Long::class) { it.toLong() }
        addConverter(String::class, Float::class) { it.toFloat() }
        addConverter(String::class, Double::class) { it.toDouble() }
    }

    private fun addDatesConverters() {
        addConverter(String::class, LocalDateTime::class) { LocalDateTime.parse(it, DateTimeFormatter.ISO_DATE_TIME) }
        addConverter(String::class, LocalDate::class) { LocalDate.parse(it, DateTimeFormatter.ISO_DATE) }
    }

    private fun addNumberConverters() {
        addConverter(Float::class, BigDecimal::class) { it.toBigDecimal() }
        addConverter(Float::class, Int::class) { it.toInt() }
        addConverter(Float::class, Long::class) { it.toLong() }
        addConverter(Float::class, Double::class) { it.toDouble() }

        addConverter(BigDecimal::class, Float::class) { it.toFloat() }
        addConverter(BigDecimal::class, Long::class) { it.toLong() }
        addConverter(BigDecimal::class, Int::class) { it.toInt() }
        addConverter(BigDecimal::class, Double::class) { it.toDouble() }

        addConverter(Long::class, BigDecimal::class) { it.toBigDecimal() }
        addConverter(Long::class, Int::class) { it.toInt() }
        addConverter(Long::class, Float::class) { it.toFloat() }
        addConverter(Long::class, Double::class) { it.toDouble() }

        addConverter(Int::class, BigDecimal::class) { it.toBigDecimal() }
        addConverter(Int::class, Long::class) { it.toLong() }
        addConverter(Int::class, Float::class) { it.toFloat() }
        addConverter(Int::class, Double::class) { it.toDouble() }

        addConverter(Double::class, BigDecimal::class) { it.toBigDecimal() }
        addConverter(Double::class, Long::class) { it.toLong() }
        addConverter(Double::class, Float::class) { it.toFloat() }
        addConverter(Double::class, Int::class) { it.toInt() }
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

        /**
         * Add a mapper function for the given property of target class
         */
        fun <V> prop(prop: KProperty1<T, V>): PropertyAsClass<F, V> {
            return PropertyAsClass(prop, this)
        }

        /**
         * Add a mapper function for the given property of target class.
         * Resulting value will be converted to target type if needed
         */
        infix fun KProperty<*>.convertAs(map: (F) -> Any?): MappingConfigBuilder<F, T> {
            val mapper = customPropertyMapper()
            mapper.addTypedMapper(this, map as (Any) -> Any?)
            return this@MappingConfigBuilder
        }

        fun addPropertyMappings(mapper: (F) -> Map<String, Any?>): MappingConfigBuilder<F, T> {
            val customPropertyMapper = customPropertyMapper()
            customPropertyMapper.multipleMapper = mapper as (Any) -> Map<String, Any?>
            return this
        }

        /**
         * Add alias for property, i.e. [this] source property will be mapped to [another] property on target object,
         * possibly with type conversion if needed
         */
        infix fun KProperty<*>.mapAs(another: KProperty<*>) {
            config.addPropertyAlias(from, to, this.name, another.name)
        }

        /**
         * [sourceName] source property will be mapped to [targetNames] on target object,
         * possibly with type conversion if needed
         */
        fun addPropertyAlias(sourceName: String, vararg targetNames: String): MappingConfigBuilder<F, T> {
            targetNames.forEach {
                config.addPropertyAlias(from, to, sourceName, it)
            }
            return this
        }

        internal fun customPropertyMapper() =
            config.customMappers.computeIfAbsent(from to to) { CustomPropertyMapper() }

    }

    /**
     * This class is needed to enforce type check at compile time, as jvm is not able to infer type from a class field
     */
    class PropertyAsClass<F : Any, V>(
        private val prop: KProperty<V>,
        private val builder: MappingConfigBuilder<F, *>
    ) {

        infix fun mapAs(value: V) {
            val mapper = builder.customPropertyMapper()
            mapper.addTypedMapper(prop) { value }
        }

        infix fun mapAs(map: (F) -> V) {
            val mapper = builder.customPropertyMapper()
            mapper.addTypedMapper(prop, map as (Any) -> Any?)
        }

    }

}
