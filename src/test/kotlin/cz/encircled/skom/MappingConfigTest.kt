package cz.encircled.skom

import kotlin.test.Test
import kotlin.test.assertEquals

class MappingConfigTest {

    @Test
    fun testBuilder() {
        val mapper = SimpleKotlinObjectMapper {
            forClasses(SimpleSource::class, SimpleTarget::class) {
                addPropertyAlias("1", "2", "3")
                addPropertyMappings {
                    mapOf()
                }
            }
        }

        val fromTo = SimpleSource::class to SimpleTarget::class

        assertEquals(setOf("2", "3"), mapper.config.aliasesForProperty(fromTo, "1"))
        assertEquals(1, mapper.config.customMappers.size)

        mapper.config().forClasses(SimpleSource::class, SimpleTarget::class) {
            addPropertyAlias("2", "3")
        }

        assertEquals(setOf("3"), mapper.config.aliasesForProperty(fromTo, "2"))
    }

}