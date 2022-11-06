package cz.encircled.skom

import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import kotlin.reflect.full.createType
import kotlin.test.Test
import kotlin.test.assertEquals

class ConverterTest {

    val mapper = SimpleKotlinObjectMapper(MappingConfig())

    @Test
    fun `default convert to string`() {
        assertEquals(12, mapper.convertValue("12", Int::class.createType()))
        assertEquals(12L, mapper.convertValue("12", Long::class.createType()))
        assertEquals(12f, mapper.convertValue("12", Float::class.createType()))
        assertEquals(BigDecimal(12), mapper.convertValue("12", BigDecimal::class.createType()))
    }

    @Test
    fun `basic numbers`() {
        val longType = Long::class.createType()
        assertEquals(12L, mapper.convertValue(12, longType))
        assertEquals(12L, mapper.convertValue(12f, longType))
        assertEquals(12L, mapper.convertValue(BigDecimal(12), longType))
        assertEquals(12L, mapper.convertValue("12", longType))

        val floatType = Float::class.createType()
        assertEquals(12f, mapper.convertValue(12, floatType))
        assertEquals(12f, mapper.convertValue(BigDecimal(12), floatType))
        assertEquals(12f, mapper.convertValue("12", floatType))

        val intType = Int::class.createType()
        assertEquals(12, mapper.convertValue(12f, intType))
        assertEquals(12, mapper.convertValue(12L, intType))
        assertEquals(12, mapper.convertValue(BigDecimal(12), intType))
        assertEquals(12, mapper.convertValue("12", intType))

        val bigDecimalType = BigDecimal::class.createType()
        assertEquals(BigDecimal(12), mapper.convertValue("12", bigDecimalType))
        assertEquals(BigDecimal(12), mapper.convertValue(12, bigDecimalType))
    }

    @Test
    fun `convert enum by name`() {
        assertEquals("ONE", mapper.convertValue(EnumFrom.ONE, String::class.createType()))
        assertEquals(EnumTo.ONE, mapper.convertValue(EnumFrom.ONE, EnumTo::class.createType()))
        assertThrows<IllegalArgumentException> {
            mapper.convertValue(EnumFrom.TWO, EnumTo::class.createType())
        }
    }

    enum class EnumFrom {
        ONE, TWO
    }

    enum class EnumTo {
        ONE
    }

}