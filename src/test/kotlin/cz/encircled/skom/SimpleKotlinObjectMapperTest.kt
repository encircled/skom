package cz.encircled.skom

import cz.encircled.skom.Extensions.mapTo
import org.junit.jupiter.api.Nested
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class SimpleKotlinObjectMapperTest {

    @Nested
    inner class MapManyToOne {

        @Test
        fun `many to one with nullable first`() {
            val mapper = SimpleKotlinObjectMapper {
                forClasses(LastName::class, CompositeName::class) {
                    LastName::lastName mapAs CompositeName::secondString
                }
            }

            val actual = mapper.mapManyTo(CompositeName::class, NullableFirstName(), FirstName("1"), LastName("2"))
            assertEquals(CompositeName("1", "2"), actual)
        }

        @Test
        fun `many to one with nullable last`() {
            val mapper = SimpleKotlinObjectMapper {
                forClasses(LastName::class, CompositeName::class) {
                    LastName::lastName mapAs CompositeName::secondString
                }
            }

            val actual = mapper.mapManyTo(CompositeName::class, FirstName("1"), LastName("2"), NullableFirstName())
            assertEquals(CompositeName("1", "2"), actual)
        }

        @Test
        fun `many to one last value wins`() {
            val mapper = SimpleKotlinObjectMapper {
                forClasses(LastName::class, CompositeName::class) {
                    LastName::lastName mapAs CompositeName::secondString
                }
            }

            val actual = mapper.mapManyTo(CompositeName::class, FirstName("1"), LastName("2"), NullableFirstName("3"))
            assertEquals(CompositeName("3", "2"), actual)
        }

    }

    @Test
    fun `map complex object`() {
        val mapper = SimpleKotlinObjectMapper {
            forClasses(Source::class, TargetEntity::class) {
                // Test multiple aliases
                Source::number mapAs TargetEntity::bodyNumber
                addPropertyAlias("collectionOfConvertable", "setOfConvertable", "mutableListOfConvertable")
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
    fun `property custom mapping with convert`() {
        val mapper = SimpleKotlinObjectMapper {
            forClasses(EntityFieldsAsGetter::class, SimpleTarget::class) {
                SimpleTarget::name convertAs { it.isBoolean }
                SimpleTarget::another convertAs { it.isBoolean }
            }
        }

        val actual = mapper.mapTo(EntityFieldsAsGetter(true, ""), SimpleTarget::class)
        assertEquals(SimpleTarget("true", "true", null), actual)
    }

    @Test
    fun `property custom mapping`() {
        val mapper = SimpleKotlinObjectMapper {
            forClasses(SimpleSource::class, SimpleTarget::class) {
                addPropertyMappings {
                    mapOf("name" to "test")
                }
                prop(SimpleTarget::another) mapAs { it.anotherName }
                prop(SimpleTarget::another) mapAs { it.anotherName }
                prop(SimpleTarget::nullableName) mapAs "23"
            }
        }

        val actual = mapper.mapTo(SimpleSource("1", "2"), SimpleTarget::class)
        assertEquals(SimpleTarget("test", "2", "23"), actual)
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
        assertTrue(actual.setWithDefault.isEmpty())

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
    fun `kotlin class descriptor`() {
        val mapper = SimpleKotlinObjectMapper {
        }
        val descriptor = mapper.getClassDescriptor<SimpleSource>(
            FromTo(TargetEntity::class, TargetEntity::class),
            TargetEntity("", 1, true, listOf(), mapOf(), mapOf(), listOf(), setOf(), mutableListOf(), null)
        )
        assertEquals(
            listOf("bodyMapOfConvertable", "bodyNumber"),
            descriptor.targetProperties.map { it.logicalName }.sorted()
        )
        assertEquals(
            listOf(
                "bodyMapOfConvertable",
                "bodyNumber",
                "bool",
                "collection",
                "collectionOfConvertable",
                "map",
                "mapOfConvertable",
                "mutableListOfConvertable",
                "name",
                "nullableName",
                "number",
                "optionalName",
                "optionalNullableName",
                "setOfConvertable",
            ),
            descriptor.sourceProperties.map { it.logicalName }.sorted()
        )
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
            descriptor.targetProperties.map { it.logicalName }.sorted()
        )
        assertEquals(
            listOf("another", "boolean", "name", "staticName"),
            descriptor.sourceProperties.map { it.logicalName }.sorted()
        )
    }

    @Test
    fun `field names as getter`() {
        val source = EntityFieldsAsGetter(true, "1")
        source.getName = "2"
        val actual = source.mapTo<EntityFieldsAsGetter>()

        assertEquals(source, actual)
        assertEquals(source.getName, actual.getName)
    }

    @Test
    fun `map with converter`() {
        val mapper = SimpleKotlinObjectMapper {
            addConverter(CompositeName::class, String::class) {
                it.firstName
            }
            forClasses(EntityCompositeName::class, SimpleTarget::class) {
                addPropertyAlias("name", "another")
            }
        }
        val source = EntityCompositeName(CompositeName("1", "2"))
        val actual = mapper.mapTo(source, SimpleTarget::class)

        assertEquals(SimpleTarget("1", "1", null), actual)
    }

    @Test
    fun `error when converter is missing`() {
        val mapper = SimpleKotlinObjectMapper {
            forClasses(EntityCompositeName::class, NestedTarget::class) {
                addPropertyAlias("name", "another")
                addPropertyAlias("name", "nested")

                addPropertyMappings {
                    mapOf("nestedCollection" to listOf<NestedTarget>())
                }
            }
        }
        val source = EntityCompositeName(CompositeName("1", "2"))
        try {
            mapper.mapTo(source, NestedTarget::class)
        } catch (e: Exception) {
            assertTrue(e.message!!.contains("No converter provided"))
            return
        }
        fail()
    }

    @Test
    fun `map directly convertable objects`() {
        val mapper = SimpleKotlinObjectMapper {
            addConverter(CompositeName::class, String::class) {
                it.firstName
            }
        }

        val actual = mapper.mapTo(CompositeName("1", "2"), String::class)
        assertEquals("1", actual)
    }

    @Test
    fun `map list of enums`() {
        val mapper = SimpleKotlinObjectMapper {
            forClasses(EntityWithEnums::class, TargetEntityWithEnums::class) {
                addPropertyAlias("testEnum", "testEnumAsStr")
                addPropertyAlias("listOfEnums", "listOfEnumsAsStr")
            }
        }

        val actual =
            mapper.mapTo(EntityWithEnums(TestEnum.SOME_VAL, listOf(TestEnum.SOME_VAL)), TargetEntityWithEnums::class)
        assertEquals(TestEnum.SOME_VAL, actual.testEnum)
        assertEquals(listOf(TestEnum.SOME_VAL), actual.listOfEnums)
        assertEquals("SOME_VAL", actual.testEnumAsStr)
        assertEquals(listOf("SOME_VAL"), actual.listOfEnumsAsStr)
    }

}