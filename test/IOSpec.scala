package io.github.timwspence.byoes

import unsafe.implicits.given
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

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

  test("auto-ceding") {
    val pool = Executors.newSingleThreadExecutor()
    given IORuntime = IORuntime(ExecutionContext.fromExecutor(pool))

    lazy val forever: IO[Unit] = IO.unit >> forever
    forever.unsafeToFuture()

    val run = IO.unit
    // If we didn't auto-cede then this would never get to run
    assertEquals(run.unsafeRunSync(), ())

    pool.shutdown()
    pool.awaitTermination(5, TimeUnit.SECONDS)
  }

  test("async") {
    val run = IO.async { cb =>
      IO.delay {
        Future {
          cb(Right(()))
        }
      }.void
    }

    assertEquals(run.unsafeRunSync(), ())
  }

}
