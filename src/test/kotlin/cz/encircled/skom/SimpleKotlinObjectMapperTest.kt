package cz.encircled.skom

import cz.encircled.skom.Extensions.mapTo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class SimpleKotlinObjectMapperTest {

    @Test
    fun `map complex object`() {
        val mapper = SimpleKotlinObjectMapper {
            forClasses(Source::class, TargetEntity::class) {
                // Test multiple aliases
                addPropertyAlias("collectionOfConvertable", "setOfConvertable")
                addPropertyAlias("collectionOfConvertable", "mutableListOfConvertable")
                addPropertyAlias("number", "number", "bodyNumber")
                addPropertyAlias("mapOfConvertable", "mapOfConvertable", "bodyMapOfConvertable")
            }
        }

        val mapped = mapper.mapTo(Source(), TargetEntity::class)

        val nestedTarget1 = NestedTarget("nestedName", null, listOf())

        val nestedTarget2 = NestedTarget("nestedName2", null, listOf())
        val expected = TargetEntity(
            "name",
            1,
            true,
            listOf("1", "2"),
            mapOf("1" to 1, "2" to 2),
            mapOf(
                "1" to nestedTarget1, "2" to NestedTarget(
                    "nestedName2",
                    NestedTarget("nestedName3", null, listOf()),
                    listOf(NestedTarget("nestedName4", null, listOf()), NestedTarget("nestedName5", null, listOf()))
                )
            ),
            listOf(nestedTarget1, nestedTarget2),
            setOf(nestedTarget1, nestedTarget2),
            mutableListOf(nestedTarget1, nestedTarget2),
            null
        )
        assertEquals(expected, mapped)

        assertEquals(1, mapped.bodyNumber)
        assertEquals(
            mapOf(
                "1" to nestedTarget1, "2" to NestedTarget(
                    "nestedName2",
                    NestedTarget("nestedName3", null, listOf()),
                    listOf(NestedTarget("nestedName4", null, listOf()), NestedTarget("nestedName5", null, listOf()))
                )
            ), mapped.bodyMapOfConvertable
        )
    }

    @Test
    fun `property alias`() {
        val mapper = SimpleKotlinObjectMapper {
            addPropertyAlias(SimpleSource::class, SimpleTarget::class, "anotherName", "another")
        }

        val actual = mapper.mapTo(SimpleSource("1", "2"), SimpleTarget::class)
        assertEquals(SimpleTarget("1", "2", null), actual)
    }

    @Test
    fun `property custom mapping`() {
        val mapper = SimpleKotlinObjectMapper {
            addMapping(SimpleSource::class, SimpleTarget::class) {
                mapOf("another" to it.anotherName)
            }
        }

        val actual = mapper.mapTo(SimpleSource("1", "2"), SimpleTarget::class)
        assertEquals(SimpleTarget("1", "2", null), actual)
    }

    @Test
    fun `mandatory prop is missing`() {
        try {
            SimpleSource("1", "2").mapTo<SimpleTarget>()
        } catch (e: IllegalStateException) {
            assertEquals("Mandatory constructor arg [another] is null!", e.message)
            return
        }
        fail()
    }

    @Test
    fun `default value on null`() {
        var actual = SimpleSource("1", "2").mapTo<SimpleTargetWithDefault>()
        assertEquals("def", actual.defaultName)

        val mapper = SimpleKotlinObjectMapper {
            addPropertyAlias(SimpleSource::class, SimpleTargetWithDefault::class, "nullableName", "defaultName")
        }

        actual = mapper.mapTo(SimpleSource("1", "2"), SimpleTargetWithDefault::class)
        assertEquals("def", actual.defaultName)

        actual = mapper.mapTo(SimpleSource("1", "2", "notDef"), SimpleTargetWithDefault::class)
        assertEquals("notDef", actual.defaultName)
    }

    @Test
    fun `map from java via getter`() {
        val source = JavaTestEntity()
        source.name = "name"
        source.another = "another"

        val actual = source.mapTo<SimpleTarget>()
        assertEquals(source.name, actual.name)
        assertEquals(source.another, actual.another)
    }

    @Test
    fun `map to java via setter`() {
        val source = SimpleSource("name", "another")

        val mapper = SimpleKotlinObjectMapper {
            forClasses(SimpleSource::class, JavaTestEntity::class) {
                addPropertyAlias("anotherName", "another")
                addPropertyMappings {
                    mapOf("boolean" to true)
                }
            }
            addPropertyAlias(SimpleSource::class, JavaTestEntity::class, "anotherName", "another")

        }

        val actual = mapper.mapTo(source, JavaTestEntity::class)
        assertTrue(actual.boolean)
        assertEquals(source.name, actual.name)
        assertEquals(source.anotherName, actual.another)
    }

    @Test
    fun `java class descriptor with getter and setter`() {
        val mapper = SimpleKotlinObjectMapper {
        }
        val descriptor = mapper.getClassDescriptor<SimpleSource>(
            FromTo(JavaTestEntity::class, JavaTestEntity::class),
            JavaTestEntity()
        )
        assertEquals(
            listOf("another", "boolean", "name"),
            descriptor.targetPropertiesByName.map { it.key }.sorted()
        )
        assertEquals(
            listOf("another", "boolean", "name"),
            descriptor.sourceProperties.map { mapper.getFieldName(it.name) }.sorted()
        )
    }

}