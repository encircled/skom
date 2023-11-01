package cz.encircled.skom

import kotlin.reflect.KCallable
import kotlin.reflect.KFunction

internal data class MappingDescriptor<T>(
    val constructor: KFunction<T>,
    val sourceProperties: Collection<KCallable<*>>,
    val targetProperties: Collection<KCallable<*>>,

    val targetPropertiesByName: Map<String, KCallable<*>>,
    val targetConstructorParamNames: List<String> = constructor.parameters.mapNotNull { it.name }
)
