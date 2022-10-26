package cz.encircled.skom

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
            Extensions.setDefaultMapper(SimpleKotlinObjectMapper(MappingConfig()))
        }
    }

    @Test
    fun `map set`() {
        val actual = setOf(SimpleSource("", "")).mapTo<SimpleTargetWithDefault>()

        assertEquals(hashSetOf(SimpleTargetWithDefault()), actual)
    }

    @Test
    fun `map list`() {
        val actual = listOf(SimpleSource("", "")).mapTo<SimpleTargetWithDefault>()

        assertEquals(listOf(SimpleTargetWithDefault()), actual)
    }

}
