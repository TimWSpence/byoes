enum IO[+A]:
  case Pure(a: A)
  case Delay(thunk: () => A)
  case FlatMap[A, B](ioa: IO[A], f: A => IO[B]) extends IO[B]

  def map[B](f: A => B): IO[B] = flatMap(x => Pure(f(x)))

  def flatMap[B](f: A => IO[B]): IO[B] = FlatMap(this, f)

  def >>[B >: A](iob: => IO[B]): IO[B] = flatMap(_ => iob)

  def unsafeRunSync(): A = this match
    case Pure(a)      => a
    case Delay(thunk) => thunk()
    case FlatMap(ioa, f) =>
      val a = ioa.unsafeRunSync()
      f(a).unsafeRunSync()

object IO:
  import IO.*

  def pure[A](a: A): IO[A] = Pure(a)

  def delay[A](thunk: => A): IO[A] = Delay(() => thunk)

  val unit: IO[Unit] = IO.pure(())
