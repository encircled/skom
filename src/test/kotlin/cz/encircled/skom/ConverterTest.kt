package cz.encircled.skom

import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import kotlin.reflect.full.createType
import kotlin.test.Test
import kotlin.test.assertEquals

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
        assertEquals(12L, converter.convertValue(BigDecimal(12), longType))
        assertEquals(12L, converter.convertValue("12", longType))

        val floatType = Float::class.createType()
        assertEquals(12f, converter.convertValue(12, floatType))
        assertEquals(12f, converter.convertValue(BigDecimal(12), floatType))
        assertEquals(12f, converter.convertValue("12", floatType))

        val intType = Int::class.createType()
        assertEquals(12, converter.convertValue(12f, intType))
        assertEquals(12, converter.convertValue(12L, intType))
        assertEquals(12, converter.convertValue(BigDecimal(12), intType))
        assertEquals(12, converter.convertValue("12", intType))

        val bigDecimalType = BigDecimal::class.createType()
        assertEquals(BigDecimal(12), converter.convertValue("12", bigDecimalType))
        assertEquals(BigDecimal(12), converter.convertValue(12, bigDecimalType))
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

    enum class EnumFrom {
        ONE, TWO, THREE
    }

    enum class EnumTo {
        ONE, FOUR
    }

}