package cz.encircled.skom

import kotlin.reflect.KProperty

class CustomPropertyMapper {

    internal var multipleMapper: (Any) -> Map<String, Any?> = { mapOf() }
    private val typedMappers: MutableMap<String, (Any) -> Any?> = mutableMapOf()

    fun addTypedMapper(prop: KProperty<*>, value: (Any) -> Any?) {
        typedMappers[prop.name] = value
    }

    fun mapProperties(source: Any): Map<String, Any?> {
        return HashMap<String, Any?>(typedMappers.size + 5).apply {
            putAll(multipleMapper.invoke(source))
            typedMappers.forEach { (key, value) ->
                this[key] = value.invoke(source)
            }
        }
    }

}