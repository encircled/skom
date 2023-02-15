package cz.encircled.skom

import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConverterTest {

    internal val converter = SimpleKotlinObjectMapper {
        addEnumMapping(EnumFrom::class, EnumTo::class, EnumFrom.THREE, EnumTo.FOUR)
    }.converter

    @Test
    fun `default convert to string`() {
        assertEquals(12, converter.convertValue("12", Int::class.createType()))
        assertEquals(12L, converter.convertValue("12", Long::class.createType()))
        assertEquals(12f, converter.convertValue("12", Float::class.createType()))
        assertEquals(BigDecimal(12), converter.convertValue("12", BigDecimal::class.createType()))
    }

    @Test
    fun `basic numbers`() {
        val longType = Long::class.createType()
        assertEquals(12L, converter.convertValue(12, longType))
        assertEquals(12L, converter.convertValue(12f, longType))
        assertEquals(12L, converter.convertValue(12.0, longType))
        assertEquals(12L, converter.convertValue(BigDecimal(12), longType))
        assertEquals(12L, converter.convertValue("12", longType))

        val floatType = Float::class.createType()
        assertEquals(12f, converter.convertValue(12, floatType))
        assertEquals(12f, converter.convertValue(12.0, floatType))
        assertEquals(12f, converter.convertValue(BigDecimal(12), floatType))
        assertEquals(12f, converter.convertValue("12", floatType))

        val intType = Int::class.createType()
        assertEquals(12, converter.convertValue(12f, intType))
        assertEquals(12, converter.convertValue(12L, intType))
        assertEquals(12, converter.convertValue(12.0, intType))
        assertEquals(12, converter.convertValue(BigDecimal(12), intType))
        assertEquals(12, converter.convertValue("12", intType))

        val bigDecimalType = BigDecimal::class.createType()
        assertEquals(BigDecimal(12), converter.convertValue("12", bigDecimalType))
        assertEquals(BigDecimal(12), converter.convertValue(12, bigDecimalType))
        assertEquals(BigDecimal(12.0).setScale(1), converter.convertValue(12.0, bigDecimalType))
        assertEquals(BigDecimal(12.0).setScale(1), converter.convertValue(12f, bigDecimalType))
    }

    @Test
    fun `convert enum by name`() {
        assertEquals("ONE", converter.convertValue(EnumFrom.ONE, String::class.createType()))

        assertEquals(EnumTo.ONE, converter.convertValue(EnumFrom.ONE, EnumTo::class.createType()))
        assertEquals(EnumFrom.ONE, converter.convertValue(EnumTo.ONE, EnumFrom::class.createType()))

        assertEquals(EnumTo.FOUR, converter.convertValue(EnumFrom.THREE, EnumTo::class.createType()))
        assertEquals(EnumFrom.THREE, converter.convertValue(EnumTo.FOUR, EnumFrom::class.createType()))

        assertThrows<IllegalArgumentException> {
            converter.convertValue(EnumFrom.TWO, EnumTo::class.createType())
        }
    }

    @Test
    fun `convert list to set`() {
        val arguments = KTypeProjection.invariant(String::class.createType())
        val setType = Set::class.createType(listOf(arguments))
        val mutSetType = MutableSet::class.createType(listOf(arguments))
        assertEquals(setOf("1"), converter.convertValue(listOf("1"), setType))

        val actualMutable = converter.convertValue(listOf("1"), mutSetType)
        assertEquals(mutableSetOf("1"), actualMutable)
        assertTrue((actualMutable as MutableSet<String>).add("2"))
    }

    enum class EnumFrom {
        ONE, TWO, THREE
    }

    enum class EnumTo {
        ONE, FOUR
    }

}