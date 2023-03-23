package io.github.timwspence.byoes

import scala.util.control.NonFatal
import scala.annotation.tailrec
import scala.concurrent.ExecutionContext

class IOFiber[A](
    val initial: IO[A],
    cb: Outcome[A] => Unit,
    initialEC: ExecutionContext
) extends Runnable:

  val autoCedeThreshold: Int = 32

  var current: IO[Any] = initial

  def run(): Unit =
    import Outcome.*
    import IO.*

    val flatMapT: Byte = 0
    val handleErrorT: Byte = 1

    var conts: List[Any => Any] = Nil
    var tags: List[Byte] = Nil

    def unwindStackTill(target: Byte): Unit =
      while (!tags.isEmpty && tags.head != target) {
        tags = tags.tail
        conts = conts.tail
      }

    def cede(io: IO[Any]): Unit =
      // Save the currently evaluating IO
      current = io
      // Re-submit to give the EC a chance to schedule a different fiber
      initialEC.execute(this)

    @tailrec
    def go(io: IO[Any], iters: Int): Any =
      if (iters == autoCedeThreshold) cede(io)
      else
        io match
          case Pure(a) =>
            unwindStackTill(flatMapT)
            conts match
              case Nil => a
              case f :: rest =>
                conts = rest
                tags = tags.tail
                val next = f(a).asInstanceOf[IO[Any]]
                go(next, iters + 1)
          case Delay(thunk) =>
            unwindStackTill(flatMapT)
            conts match
              case Nil => thunk()
              case f :: rest =>
                conts = rest
                tags = tags.tail
                val next = f(thunk()).asInstanceOf[IO[Any]]
                go(next, iters + 1)
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
              case Nil => throw e
              case f :: rest =>
                conts = rest
                tags = tags.tail
                val next = f(e).asInstanceOf[IO[Any]]
                go(next, iters + 1)
          case Cede =>
            cede(io)

    var result: Outcome[A] = null
    try {
      result = Succeeded(go(current, 0).asInstanceOf[A])
    } catch {
      case NonFatal(e) => result = Errored(e)
    }

    cb(result)

enum Outcome[A]:
  case Succeeded(value: A)
  case Errored(error: Throwable)
