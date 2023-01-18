package cz.encircled.skom

data class NestedSource(
    val name: String = "nestedName",
    val nested: NestedSource? = null,
    val nestedCollection: List<NestedSource> = listOf(),
) : Convertable

data class Source(
    val name: String = "name",
    val number: Int = 1,
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
) : Convertable


data class NestedTarget(
    val name: String,
    val nested: NestedTarget?,
    val nestedCollection: List<NestedTarget>,
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
)

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
)
