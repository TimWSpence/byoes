package io.github.timwspence.byoes

import scala.util.control.NonFatal
import scala.annotation.tailrec
import scala.concurrent.ExecutionContext
import java.util.concurrent.atomic.AtomicBoolean

class IOFiber[A](
    val initial: IO[A],
    cb: Outcome[A] => Unit,
    initialEC: ExecutionContext
) extends Runnable:
  import Constants.*

  val autoCedeThreshold: Int = 32

  val acquire = new AtomicBoolean(false)

  // This tracks the IO we are currently executing. Necessary for maintaining
  // state when we are de-scheduled due to a cede or async boundary
  var current: IO[Any] = initial
  var conts: List[Any => Any] = Nil
  var tags: List[Byte] = Nil

  def run(): Unit =
    import Outcome.*
    import IO.*

    if (acquire.getAndSet(true)) {
      // Someone else has acquired the runloop
    } else {

      def unwindStackTill(target: Byte): Unit =
        while (
          !tags.isEmpty && tags.head != target && tags.head != asyncReturnT
        ) {
          tags = tags.tail
          conts = conts.tail
        }

      def cede(io: IO[Any]): Unit =
        // Save the currently evaluating IO
        current = io
        acquire.set(false)
        // Re-submit to give the EC a chance to schedule a different fiber
        initialEC.execute(this)

      def endAsyncRegistration(): Unit =
        conts = conts.tail
        tags = tags.tail
        acquire.set(false)
        ()

      @tailrec
      def go(io: IO[Any], iters: Int): Any =
        if (iters == autoCedeThreshold) cede(io)
        else
          io match
            case Pure(a) =>
              unwindStackTill(flatMapT)
              conts match
                case Nil => succeeded(a)
                case f :: rest =>
                  if (tags.head == asyncReturnT) {
                    endAsyncRegistration()
                  } else {
                    conts = rest
                    tags = tags.tail
                    val next = f(a).asInstanceOf[IO[Any]]
                    go(next, iters + 1)
                  }
            case Delay(thunk) =>
              unwindStackTill(flatMapT)
              conts match
                case Nil => succeeded(thunk())
                case f :: rest =>
                  if (tags.head == asyncReturnT) {
                    endAsyncRegistration()
                  } else {
                    conts = rest
                    tags = tags.tail
                    val next = f(thunk()).asInstanceOf[IO[Any]]
                    go(next, iters + 1)
                  }
            case FlatMap(io, f) =>
              conts = f.asInstanceOf[Any => Any] :: conts
              tags = flatMapT :: tags
              go(io, iters + 1)
            case HandleErrorWith(io, f) =>
              conts = f.asInstanceOf[Any => Any] :: conts
              tags = handleErrorT :: tags
              go(io, iters + 1)
            case RaiseError(e) =>
              unwindStackTill(handleErrorT)
              conts match
                case Nil => errored(e)
                case f :: rest =>
                  if (tags.head == asyncReturnT) {
                    // TODO what are the semantics of an unhandled error in async registration?
                    endAsyncRegistration()
                  } else {
                    conts = rest
                    tags = tags.tail
                    val next = f(e).asInstanceOf[IO[Any]]
                    go(next, iters + 1)
                  }
            case Cede =>
              cede(io)
            case Async(run) =>
              val callback: Either[Throwable, Any] => Unit =
                (res: Either[Throwable, Any]) =>
                  res match
                    // This runs on a foreign EC. Do we need to worry about memory visibility?
                    case Right(v) =>
                      // TODO this could publish the result somewhere and then we could attempt
                      // to continue running when we hit an asynn return in our stack
                      while (acquire.getAndSet(true)) {}
                      current = IO.pure(v)
                      initialEC.execute(this)
                      acquire.set(false)
                    case Left(e) =>
                      while (acquire.getAndSet(true)) {}
                      current = IO.raiseError(e)
                      initialEC.execute(this)
                      acquire.set(false)
              conts = asyncReturn :: conts
              tags = asyncReturnT :: tags
              val next = run(callback)
              go(next, iters + 1)

      go(current, 0)
    }

  def succeeded(value: Any): Unit =
    // acquire.set(false)
    cb(Outcome.Succeeded(value.asInstanceOf[A]))

  def errored(e: Throwable): Unit =
    // acquire.set(false)
    cb(Outcome.Errored(e))

enum Outcome[A]:
  case Succeeded(value: A)
  case Errored(error: Throwable)

object Constants:
  val flatMapT: Byte = 0
  val handleErrorT: Byte = 1
  val asyncReturnT: Byte = 2

  val asyncReturn: Any => Any = _ => ()
