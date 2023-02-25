package cz.encircled.skom

import java.util.*
import java.util.function.Supplier
import kotlin.reflect.KClass

object Extensions {

    var mapper: SimpleKotlinObjectMapper = SimpleKotlinObjectMapper { }

    inline fun <reified T : Any> List<Convertable>.mapTo(): List<T> {
        val klass = T::class
        return map { mapper.mapTo(it, klass) }
    }

    fun <T : Any> List<Convertable>.mapTo(klass : KClass<T>): List<T> {
        return map { mapper.mapTo(it, klass) }
    }

    inline fun <reified T : Any> Set<Convertable>.mapTo(): Set<T> {
        val klass = T::class
        val result = HashSet<T>(size)
        forEach { result.add(mapper.mapTo(it, klass)) }
        return result
    }

    fun <T : Any> Set<Convertable>.mapTo(klass : KClass<T>): Set<T> {
        val result = HashSet<T>(size)
        forEach { result.add(mapper.mapTo(it, klass)) }
        return result
    }

    inline fun <reified T : Any> Convertable.mapTo(): T {
        return mapper.mapTo(this, T::class)
    }

    fun <T : Any> Convertable.mapTo(klass : KClass<T>): T {
        return mapper.mapTo(this, klass)
    }

    inline fun <reified T : Any> Optional<Convertable>.mapToNullable(): T? {
        return map { mapper.mapTo(it, T::class) }.orElse(null)
    }

    inline fun <reified T : Any> Optional<Convertable>.mapTo(
        orElse: Supplier<Exception> = Supplier {
            IllegalStateException("Source object must be not null")
        }
    ): T {
        return map { mapper.mapTo(it, T::class) }.orElseThrow(orElse)
    }

    fun setDefaultMapper(mapper: SimpleKotlinObjectMapper) {
        if (mapper.config.classToDescriptor.isEmpty()) {
            mapper.config.classToDescriptor = this.mapper.config.classToDescriptor
        }
        this.mapper = mapper
    }

}