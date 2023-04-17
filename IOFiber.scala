package io.github.timwspence.byoes

import scala.util.control.NonFatal
import scala.annotation.tailrec
import scala.concurrent.ExecutionContext
import java.util.concurrent.atomic.AtomicBoolean

class IOFiber[A](
    private val initial: IO[A],
    private val cb: Outcome[A] => Unit,
    private val initialEC: ExecutionContext
) extends Runnable:
  import Constants.*

  private val autoCedeThreshold: Int = 32

  // Async registration introduces a potential race between
  // the async registration block and the re-scheduled fiber. Both
  // operate on the non-atomic mutable internals of the fiber so this
  // functions effectively as a mutex and functions as a barrier that
  // ensures the internals are correctly published when the runloop is yielded
  private val acquire = new AtomicBoolean(false)

  // This tracks the IO we are currently executing. Necessary for maintaining
  // state when we are de-scheduled due to a cede or async boundary
  private var current: IO[Any] = initial
  private var conts: List[Any => Any] = Nil
  private var tags: List[Byte] = Nil

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

      // We de-limit async registration regions on the stack via a
      // special marker tag. When unwinding the stack to find the next
      // continuation, you should not unwind past one of these markers
      def endAsyncRegistration(): Unit =
        conts = conts.tail
        tags = tags.tail
        acquire.set(false)
        ()

      def pushStack(tag: Byte, f: Any => Any): Unit =
        tags = tag :: tags
        conts = f :: conts

      def popStack(): Any => Any =
        val f = conts.head
        tags = tags.tail
        conts = conts.tail
        f

      @tailrec
      def go(io: IO[Any], iters: Int): Any =
        if (iters == autoCedeThreshold) cede(io)
        else
          io match
            case Pure(a) =>
              unwindStackTill(flatMapT)
              conts match
                case Nil => succeeded(a)
                case _ =>
                  if (tags.head == asyncReturnT) {
                    endAsyncRegistration()
                  } else {
                    val f = popStack()
                    val next = f(a).asInstanceOf[IO[Any]]
                    go(next, iters + 1)
                  }
            case Delay(thunk) =>
              unwindStackTill(flatMapT)
              conts match
                case Nil => succeeded(thunk())
                case _ =>
                  if (tags.head == asyncReturnT) {
                    endAsyncRegistration()
                  } else {
                    val f = popStack()
                    val next = f(thunk()).asInstanceOf[IO[Any]]
                    go(next, iters + 1)
                  }
            case FlatMap(io, f) =>
              pushStack(flatMapT, f.asInstanceOf[Any => Any])
              go(io, iters + 1)
            case HandleErrorWith(io, f) =>
              pushStack(handleErrorT, f.asInstanceOf[Any => Any])
              go(io, iters + 1)
            case RaiseError(e) =>
              unwindStackTill(handleErrorT)
              conts match
                case Nil => errored(e)
                case _ =>
                  if (tags.head == asyncReturnT) {
                    // TODO what are the semantics of an unhandled error in async registration?
                    endAsyncRegistration()
                  } else {
                    val f = popStack()
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
              pushStack(asyncReturnT, asyncReturn)
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
