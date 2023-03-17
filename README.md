# Chapter 1

The goal here is to build a basic `IO` data-type that supports `pure`, `delay`
and `flatMap` and can be evaluated synchronously via `unsafeRunSync()`.

```scala
val program = IO.pure(42).flatMap { n =>
  IO.delay(println(s"Got $n"))
}

program.unsafeRunSync()
```
