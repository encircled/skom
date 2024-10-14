[![CI build](https://github.com/encircled/skom/actions/workflows/run-tests-action.yml/badge.svg)](https://github.com/encircled/skom/actions/workflows/run-tests-action.yml)
[![codecov](https://codecov.io/gh/encircled/skom/branch/main/graph/badge.svg)](https://codecov.io/gh/encircled/skom)
[![Maven Central](https://img.shields.io/maven-central/v/cz.encircled/skom.svg?label=Maven%20Central)](https://search.maven.org/artifact/cz.encircled/skom/1.16/jar)

# Simple Kotlin Object Mapper

Kotlin reflection-based object mapping

## TL;DR

```kotlin
data class From(
    val age: Int,
    val name: String
) : Convertable

data class To(
    val age: Int,
    val name: String
)

val mapped: To = From(1, "John").mapTo()
assertEquals(To(1, "John"), mapped)
```

```xml
<dependency>
    <groupId>cz.encircled</groupId>
    <artifactId>skom</artifactId>
    <version>1.15</version>
</dependency>
```

## Setup

SKOM offers extension functions for objects that implement the `cz.encircled.skom.Convertable` interface:

- `val mapped : To = From(...).mapTo()`
- `val mapped : List<To> = listOf(From(...)).mapTo()`
- `val mapped : To = listOf(From1(...), From2(...)).mapManyTo()`

it uses an instance of SKOM with the default configuration, which can be overridden via:

```kotlin
Extensions.setDefaultMapper(SimpleKotlinObjectMapper {
    // configuration goes here
    forClasses(From::class, To::class) {
        From::name mapAs To::firstName
    }
})
```

Alternatively, an instance of `SimpleKotlinObjectMapper` can be used directly

## Property alias

```kotlin
data class From(
    val firstName: String
) : Convertable

data class To(
    val name: String
)

val mapper = SimpleKotlinObjectMapper {
    forClasses(From::class, To::class) {
        From::firstName mapAs To::name

        // Or multiple aliases
        addPropertyAlias("firstName", "name", "name2")
    }
}

val mapped: To = From("John").mapTo()
assertEquals(To("John"), mapped)
```

## Custom dynamic mapping

```kotlin
data class From(
    val firstName: String,
    val lastName: String
) : Convertable

data class To(
    val name: String
)

val mapper = SimpleKotlinObjectMapper {
    forClasses(From::class, To::class) {
        prop(To::name) mapAs { "${it.firstName} ${it.lastName}" }

        // [someNumber] will be converted to string
        To::name convertAs { it.someNumber }

        // Or to share mapping between multiple properties  
        addPropertyMappings {
            val fullName = compute(it)
            mapOf("name" to fullName, "name2" to fullName)
        }
    }
}

val mapped: To = From("John", "Snow").mapTo()
assertEquals(To("John Snow"), mapped)
```

## Custom value converter

```kotlin

import java.math.BigDecimal

data class From(
    val amount: BigDecimal
) : Convertable

data class To(
    val price: String
)

val mapper = SimpleKotlinObjectMapper {
    addConverter(BigDecimal::class, String::class) {
        "$amount $"
    }
}

val mapped: To = From(BigDecimal(123)).mapTo()
assertEquals(To("123 $"), mapped)
```

## Enum mapping

Enums are mapped out of the box using their names.
Custom enums mapping can be added via:

```kotlin
SimpleKotlinObjectMapper {
    addEnumMapping(EnumA::class, EnumB::class, EnumA.A, EnumB.B)
}
```

## Complex configuration

Mapping configuration can be set during the creation of the SKOM instance:

```kotlin
SimpleKotlinObjectMapper {
    forClasses(A::class, B::class) {
        // ...
    }

    forClasses(C::class, D::class) {
        // ...
    }

    forClasses(E::class, F::class) {
        // ...
    }
}
```

or added after, as following:

```kotlin
val mapper = SimpleKotlinObjectMapper { }
val config = mapper.config()

config.forClasses(A::class, B::class) {
    // ...
}

config.forClasses(C::class, D::class) {
    // ...
}

config.forClasses(E::class, F::class) {
    // ...
}
```

## Performance

The reflection-based implementation is not blazing fast, so it is not recommended for mapping a large number of objects (100k+)
