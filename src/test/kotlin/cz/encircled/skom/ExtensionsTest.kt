package cz.encircled.skom

import cz.encircled.skom.Extensions.mapManyTo
import cz.encircled.skom.Extensions.mapTo
import kotlin.test.Test
import kotlin.test.assertEquals

class ExtensionsTest {

    @Test
    fun `set default mapper`() {
        try {
            Extensions.setDefaultMapper(SimpleKotlinObjectMapper {
                addPropertyAlias(SimpleSource::class, SimpleTarget::class, "anotherName", "another")
            })
            val actual = SimpleSource("1", "2").mapTo<SimpleTarget>()

            assertEquals(SimpleTarget("1", "2", null), actual)
        } finally {
            Extensions.setDefaultMapper(SimpleKotlinObjectMapper {})
        }
    }

    @Test
    fun `map single object`() {
        val actual = SimpleSource("", "").mapTo<SimpleTargetWithDefault>()
        val actual2 = SimpleSource("", "").mapTo(SimpleTargetWithDefault::class)

        assertEquals(SimpleTargetWithDefault(), actual)
        assertEquals(SimpleTargetWithDefault(), actual2)
    }

    @Test
    fun `map a set`() {
        val actual = setOf(SimpleSource("", "")).mapTo<SimpleTargetWithDefault>()
        val actual2 = setOf(SimpleSource("", "")).mapTo(SimpleTargetWithDefault::class)

        assertEquals(hashSetOf(SimpleTargetWithDefault()), actual)
        assertEquals(hashSetOf(SimpleTargetWithDefault()), actual2)
    }

    @Test
    fun `map a list`() {
        val actual = listOf(SimpleSource("", "")).mapTo<SimpleTargetWithDefault>()
        val actual2 = listOf(SimpleSource("", "")).mapTo(SimpleTargetWithDefault::class)

        assertEquals(listOf(SimpleTargetWithDefault()), actual)
        assertEquals(listOf(SimpleTargetWithDefault()), actual2)
    }

    @Test
    fun `map many to one`() {
        val actual = listOf(
            SimpleSource("a", "", "c"),
            SimpleSource("a", ""),
            SimpleSource("a", "b")
        ).mapManyTo(SimpleSource::class)
        assertEquals(SimpleSource("a", "b", "c"), actual)
    }

}
