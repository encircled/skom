package cz.encircled.skom

import java.math.BigDecimal

data class NestedSource(
    val name: String = "nestedName",
    val nested: NestedSource? = null,
    val nestedCollection: List<NestedSource> = listOf(),
) : Convertable

data class ParametrizedSource<T>(
    val param: T
) : Convertable

data class Source(
    val name: String = "name",
    val number: BigDecimal = BigDecimal.ONE,
    val bool: Boolean = true,
    val collection: List<String> = listOf("1", "2"),
    val map: Map<String, Int> = mapOf("1" to 1, "2" to 2),
    val mapOfConvertable: Map<String, NestedSource> = mapOf(
        "1" to NestedSource(),
        "2" to NestedSource(
            "nestedName2",
            NestedSource("nestedName3"),
            listOf(NestedSource("nestedName4"), NestedSource("nestedName5"))
        )
    ),
    val collectionOfConvertable: List<NestedSource> = listOf(NestedSource(), NestedSource("nestedName2")),
    val parametrizedSource: ParametrizedSource<Int> = ParametrizedSource(123)
) : Convertable


data class NestedTarget(
    val name: String,
    val nested: NestedTarget?,
    val nestedCollection: List<NestedTarget>,
)

data class ParametrizedTarget<T>(
    val param: T
)


data class TargetEntity(
    val name: String,
    val number: Int,
    val bool: Boolean,
    val collection: List<String>,
    val map: Map<String, Int>,
    val mapOfConvertable: Map<String, NestedTarget>,
    val collectionOfConvertable: List<NestedTarget>,
    val setOfConvertable: Set<NestedTarget>,
    val mutableListOfConvertable: MutableList<NestedTarget>,
    val nullableName: String?,
    val optionalName: String = "optional",
    val optionalNullableName: String? = "optionalNullable",
    val parametrizedTarget: ParametrizedTarget<Int>? = null
) {
    var bodyNumber: Int? = null
    var bodyMapOfConvertable: Map<String, NestedTarget>? = null
}

data class SimpleSource(
    val name: String,
    val anotherName: String,
    val nullableName: String? = null,
) : Convertable

data class SimpleTarget(
    val name: String,
    val another: String,
    val nullableName: String?,
)

data class SimpleTargetWithDefault(
    val defaultName: String = "def",
    var setWithDefault: MutableSet<SimpleTarget> = mutableSetOf()
)

data class EntityFieldsAsGetter(
    val isBoolean: Boolean,
    val setName: String,
) : Convertable {
    var getName: String = ""
}

data class EntityCompositeName(
    val name: CompositeName
) : Convertable

data class CompositeName(
    val firstName: String,
    val secondString: String,
)

data class EntityWithEnums(
    val testEnum: TestEnum = TestEnum.SOME_VAL,
    val listOfEnums: List<TestEnum> = listOf(),
) : Convertable

data class TargetEntityWithEnums(
    val testEnum: TestEnum,
    val listOfEnums: List<TestEnum>,
    val testEnumAsStr: String,
    val listOfEnumsAsStr: List<String>,
)

enum class TestEnum {
    SOME_VAL;
}

class WithSelfReferenceSource : Convertable {
    var self: WithSelfReferenceSource? = null
}

class WithSelfReferenceTarget {
    var self: WithSelfReferenceTarget? = null
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WithSelfReferenceTarget

        return (self == null) == (other.self == null)
    }

    override fun hashCode(): Int {
        return if (self != null) 1 else 0
    }

}

data class NullableFirstName(val firstName: String? = null)
data class FirstName(val firstName: String)
data class LastName(val lastName: String)