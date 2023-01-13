package cz.encircled.skom

import kotlin.reflect.KFunction
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty1

internal data class MappingDescriptor<T>(
    val constructor: KFunction<T>,
    val sourceProperties: Collection<KProperty1<*, *>>,
    val targetProperties: Collection<KMutableProperty<*>>,
    val targetPropertiesByName: Map<String, KMutableProperty<*>> = targetProperties.associateBy { it.name },
    val targetConstructorParamNames: List<String> = constructor.parameters.mapNotNull { it.name }
)
