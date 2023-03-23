class IOSpec extends munit.FunSuite {

  test("pure") {
    assertEquals(IO.pure(1).unsafeRunSync(), 1)
  }

  test("delay is referentially transparent") {
    var x = 0

    val run = IO.delay {
      x = x + 1
    }

    (run >> run).unsafeRunSync()

    assertEquals(x, 2)
  }

  test("flatMap") {
    val run = IO.pure(1).flatMap { n => IO.pure(n + 1) }

    assertEquals(run.unsafeRunSync(), 2)
  }

  test("stack safe") {

    def go(n: Int): IO[Unit] =
      if (n == 1) IO.unit else IO.unit.flatMap(_ => go(n - 1))

    go(20000).unsafeRunSync()

  }

  test("handle errors") {

    val run = IO
      .raiseError[String](new RuntimeException("boom"))
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

}
