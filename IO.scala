package io.github.timwspence.byoes

import scala.annotation.tailrec
import scala.concurrent.{Future, Promise}
import scala.concurrent.Await
import scala.concurrent.duration.Duration

enum IO[+A]:
  case Pure(a: A)
  case Delay(thunk: () => A)
  case FlatMap[A, B](ioa: IO[A], f: A => IO[B]) extends IO[B]
  case RaiseError(e: Throwable)
  case HandleErrorWith[A, B >: A](ioa: IO[A], f: Throwable => IO[B])
      extends IO[B]

  def map[B](f: A => B): IO[B] = flatMap(x => Pure(f(x)))

  def flatMap[B](f: A => IO[B]): IO[B] = FlatMap(this, f)

  def >>[B >: A](iob: => IO[B]): IO[B] = flatMap(_ => iob)

  def handleErrorWith[B >: A](f: Throwable => IO[B]): IO[B] =
    HandleErrorWith(this, f)

  def unsafeToFuture()(using runtime: IORuntime): Future[A] =
    val promise = Promise[A]()
    val cb: Outcome[A] => Unit = {
      case Outcome.Succeeded(value) => promise.trySuccess(value)
      case Outcome.Errored(e)       => promise.tryFailure(e)
    }
    val fiber = new IOFiber(this, cb)
    runtime.ec.execute(fiber)
    promise.future

  def unsafeRunSync()(using runtime: IORuntime): A =
    val future = unsafeToFuture()
    Await.result(future, Duration.Inf)

object IO:
  import IO.*

  def pure[A](a: A): IO[A] = Pure(a)

  def delay[A](thunk: => A): IO[A] = Delay(() => thunk)

  val unit: IO[Unit] = IO.pure(())

  def raiseError[A](e: Throwable): IO[A] = RaiseError(e)
