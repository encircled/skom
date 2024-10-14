package cz.encircled.skom

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

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

    @Test
    fun testParametrized() {
        val c = MappingConfig()
        c.addConverter(ParametrizedObj::class, AnotherParametrizedObj::class) {
            AnotherParametrizedObj(it.param)
        }

        val converter = c.directConverter(ParametrizedObj(""), TypeWrapper(AnotherParametrizedObj::class.java))
        assertNotNull(converter)
    }

    class ParametrizedObj<T>(val param: T)

    class AnotherParametrizedObj<T>(val param: T)

}