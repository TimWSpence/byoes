# Chapter 3

The goal of this chapter is to add the `raiseError` and `handleErrorWith`
operations. The following tests should pass:

```scala
test("handle errors") {

  val run = IO.raiseError[String](new RuntimeException("boom"))
              .handleErrorWith(e => IO.pure(e.getMessage()))

  assertEquals(run.unsafeRunSync(), "boom")

}

test("unhandled errors") {

   val run = IO
     .raiseError[String](new RuntimeException("boom"))

   interceptMessage[RuntimeException]("boom") {
     run.unsafeRunSync()
   }
}
```
