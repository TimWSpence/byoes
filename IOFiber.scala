package io.github.timwspence.byoes

import scala.util.control.NonFatal
import scala.annotation.tailrec

class IOFiber[A](val initial: IO[A], cb: Outcome[A] => Unit) extends Runnable:

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

    @tailrec
    def go(io: IO[Any]): Any = io match
      case Pure(a) =>
        unwindStackTill(flatMapT)
        conts match
          case Nil => a
          case f :: rest =>
            conts = rest
            tags = tags.tail
            val next = f(a).asInstanceOf[IO[Any]]
            go(next)
      case Delay(thunk) =>
        unwindStackTill(flatMapT)
        conts match
          case Nil => thunk()
          case f :: rest =>
            conts = rest
            tags = tags.tail
            val next = f(thunk()).asInstanceOf[IO[Any]]
            go(next)
      case FlatMap(io, f) =>
        conts = f.asInstanceOf[Any => Any] :: conts
        tags = flatMapT :: tags
        go(io)
      case HandleErrorWith(io, f) =>
        conts = f.asInstanceOf[Any => Any] :: conts
        tags = handleErrorT :: tags
        go(io)
      case RaiseError(e) =>
        unwindStackTill(handleErrorT)
        conts match
          case Nil => throw e
          case f :: rest =>
            conts = rest
            tags = tags.tail
            val next = f(e).asInstanceOf[IO[Any]]
            go(next)

    var result: Outcome[A] = null
    try {
      result = Succeeded(go(initial).asInstanceOf[A])
    } catch {
      case NonFatal(e) => result = Errored(e)
    }

    cb(result)

enum Outcome[A]:
  case Succeeded(value: A)
  case Errored(error: Throwable)
