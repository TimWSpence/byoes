import scala.annotation.tailrec

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

  def unsafeRunSync(): A =
    var conts: List[Any => Any] = Nil

    def go(io: IO[Any]): Any = io match
      case Pure(a) =>
        conts match
          case Nil => a
          case f :: rest =>
            conts = rest
            val next = f(a).asInstanceOf[IO[Any]]
            go(next)
      case Delay(thunk) =>
        conts match
          case Nil => thunk()
          case f :: rest =>
            conts = rest
            val next = f(thunk()).asInstanceOf[IO[Any]]
            go(next)
      case FlatMap(io, f) =>
        conts = f.asInstanceOf[Any => Any] :: conts
        go(io)

    go(this).asInstanceOf[A]

object IO:
  import IO.*

  def pure[A](a: A): IO[A] = Pure(a)

  def delay[A](thunk: => A): IO[A] = Delay(() => thunk)

  val unit: IO[Unit] = IO.pure(())

  def raiseError[A](e: Throwable): IO[A] = RaiseError(e)
