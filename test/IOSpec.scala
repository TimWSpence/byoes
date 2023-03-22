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

}
