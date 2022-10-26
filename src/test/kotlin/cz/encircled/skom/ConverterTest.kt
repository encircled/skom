package cz.encircled.skom

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
        assertEquals(12L, mapper.convertValue(12, Long::class.createType()))
        assertEquals(12L, mapper.convertValue(12f, Long::class.createType()))
        assertEquals(12L, mapper.convertValue(BigDecimal(12), Long::class.createType()))
        assertEquals(12L, mapper.convertValue("12", Long::class.createType()))

        assertEquals(12f, mapper.convertValue(12, Float::class.createType()))
        assertEquals(12f, mapper.convertValue(BigDecimal(12), Float::class.createType()))
        assertEquals(12f, mapper.convertValue("12", Float::class.createType()))

        assertEquals(12, mapper.convertValue(12f, Int::class.createType()))
        assertEquals(12, mapper.convertValue(12L, Int::class.createType()))
        assertEquals(12, mapper.convertValue(BigDecimal(12), Int::class.createType()))
        assertEquals(12, mapper.convertValue("12", Int::class.createType()))

        assertEquals(BigDecimal(12), mapper.convertValue("12", BigDecimal::class.createType()))
        assertEquals(BigDecimal(12), mapper.convertValue(12, BigDecimal::class.createType()))
    }

}