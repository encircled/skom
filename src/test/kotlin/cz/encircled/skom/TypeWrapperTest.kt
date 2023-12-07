package cz.encircled.skom

import kotlin.reflect.jvm.javaType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TypeWrapperTest {

    val listOfStrings: List<String> = listOf()

    val mapStringToInt: Map<String, Int> = mapOf()

    val listOfEnums: List<TestEnum> = listOf()

    @Test
    fun `plain class`() {
        val wrapper = TypeWrapper(Int::class.java)
        assertEquals(Int::class.java, wrapper.rawClass())
        assertFalse(wrapper.isEnum())
        assertFalse(wrapper.isParametrized())
    }

    @Test
    fun `enum class`() {
        val wrapper = TypeWrapper(TestEnum::class.java)
        assertEquals(TestEnum::class.java, wrapper.rawClass())
        assertTrue(wrapper.isEnum())
        assertFalse(wrapper.isParametrized())
    }

    @Test
    fun `list with single generic`() {
        val wrapper = TypeWrapper(this::listOfStrings.returnType.javaType)
        assertEquals(List::class.java, wrapper.rawClass())
        assertFalse(wrapper.isEnum())
        assertTrue(wrapper.isParametrized())
        assertEquals(String::class.java, wrapper.typeArgument(0))
    }

    @Test
    fun `list of enums`() {
        val wrapper = TypeWrapper(this::listOfEnums.returnType.javaType)
        assertEquals(List::class.java, wrapper.rawClass())
        assertFalse(wrapper.isEnum())
        assertTrue(wrapper.isParametrized())
        assertEquals(TestEnum::class.java, wrapper.typeArgument(0))
    }

    @Test
    fun `map with two generics`() {
        val wrapper = TypeWrapper(this::mapStringToInt.returnType.javaType)
        assertEquals(Map::class.java, wrapper.rawClass())
        assertFalse(wrapper.isEnum())
        assertTrue(wrapper.isParametrized())
        assertEquals(String::class.java, wrapper.typeArgument(0))
        assertEquals("java.lang.Integer", wrapper.typeArgument(1).typeName)
    }

}