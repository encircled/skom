package cz.encircled.skom

object Extensions {

    var mapper: SimpleKotlinObjectMapper = SimpleKotlinObjectMapper(MappingConfig())

    inline fun <reified T : Any> List<Convertable>.mapTo(): List<T> {
        val klass = T::class
        return map { mapper.mapTo(it, klass) }
    }

    inline fun <reified T : Any> Set<Convertable>.mapTo(): Set<T> {
        val klass = T::class
        val result = HashSet<T>(size)
        forEach { result.add(mapper.mapTo(it, klass)) }
        return result
    }

    inline fun <reified T : Any> Convertable.mapTo(): T {
        return mapper.mapTo(this, T::class)
    }

    fun setDefaultMapper(mapper: SimpleKotlinObjectMapper) {
        if (mapper.config.classToDescriptor.isEmpty()) {
            mapper.config.classToDescriptor = this.mapper.config.classToDescriptor
        }
        this.mapper = mapper
    }

}