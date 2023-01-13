[![CI build](https://github.com/encircled/skom/actions/workflows/run-tests-action.yml/badge.svg)](https://github.com/encircled/skom/actions/workflows/run-tests-action.yml)
[![codecov](https://codecov.io/gh/encircled/skom/branch/main/graph/badge.svg)](https://codecov.io/gh/encircled/skom)

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

## Setup

SKOM offers extension functions for objects implementing `cz.encircled.skom.Convertable` interface:

- `val mapped : To = From(...).mapTo()`
- `val mapped : List<To> = listOf(From(...)).mapTo()`

it uses the instance of SKOM with default configuration, which can be overridden via:

```kotlin
Extensions.setDefaultMapper(SimpleKotlinObjectMapper {
    // config setup here
})
```

Alternatively, instance of `SimpleKotlinObjectMapper` can be used directly

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
        addPropertyAlias("firstName", "name")
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
        addPropertyMappings {
            mapOf("name" to "${it.firstName} ${it.lastName}")
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

val mapped: To = From(123).mapTo()
assertEquals(To("123 $"), mapped)
```

## Enum mapping

Enums are mapped out of the box using the name.

Custom enums mapping is added via:

```kotlin
SimpleKotlinObjectMapper {
    forClasses(EnumA::class, EnumB::class) {
        addEnumMapping(EnumA.A, EnumB.B)
    }
}
```

## Complex configuration

Configuration of multiple entities might be set during SKOM creation

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

or could be split like:

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

Reflection-based implementation is not very fast, approx 150 ops/ms for complex objects, thus it is not recommended for
mapping high amount of objects (10k+)
