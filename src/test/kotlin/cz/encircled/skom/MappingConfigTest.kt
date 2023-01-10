package cz.encircled.skom

import kotlin.test.Test
import kotlin.test.assertEquals

class MappingConfigTest {

    @Test
    fun testBuilder() {
        val mapper = SimpleKotlinObjectMapper {
            forClasses(SimpleSource::class, SimpleTarget::class) {
                addPropertyAlias("1", "2")
                addPropertyMappings {
                    mapOf()
                }
            }
        }

        assertEquals(1, mapper.config.propertyAliases.size)
        assertEquals(1, mapper.config.customMappers.size)
    }

}