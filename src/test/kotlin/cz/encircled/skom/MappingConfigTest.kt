package cz.encircled.skom

import kotlin.test.Test
import kotlin.test.assertEquals

class MappingConfigTest {

    @Test
    fun testBuilder() {
        val conf = MappingConfig().builder(SimpleSource::class, SimpleTarget::class)
            .addPropertyAlias("1", "2")
            .addMapping {
                mapOf()
            }
            .config()

        assertEquals(1, conf.propertyAliases.size)
        assertEquals(1, conf.customMappers.size)
    }

}