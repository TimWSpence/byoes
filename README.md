# Build your own effect system

This is a course designed to help you build an effect system in Scala that is
similar to [Cats Effect](https://typelevel.org/cats-effect/). The intention is
that by building it yourself you should gain a deeper understanding of how an
`IO` action is evaluated and the impact of things like blocking, timers, async
execution on the performance of your application. The assumption is that you
are familiar with using a library like Cats Effect so usage of an effect system
is not explained.

The intention is that by the end you should be able to understand the majority
of the
[`IOFiber`](https://github.com/typelevel/cats-effect/blob/series/3.x/core/shared/src/main/scala/cats/effect/IOFiber.scala)
implementation and have at least a high-level understanding of the design of
the work-stealing threadpool.

## How to use

The course is divided into chapters which should iteratively build on top of
each other. Each chapter has its own git branch. The goal of that chapter will
be described in the corresponding `README.md`. If you get stuck then you can
consult `hints.md` for some guidance. If you're really stuck then you can look
at the source code, which is a possible solution for that chapter.

The task description in the README is left deliberately open-ended to encourage
you to explore possible solutions but the provided solutions will be similar to
what Cats Effect does (albeit simplified).

## Chapters

NB: Only the first 3 chapters are implemented thus far.

* Chapter 1 - basic "free" monad with suspension of synchronous effects (`SyncIO`)
* Chapter 2 - stack safety
* Chapter 3 - error handling
* Chapter 4 - fixed threadpool runtime
* Chapter 5 - cede / auto-cede
* Chapter 6 - async
* Chapter 7 - forking fibers
* Chapter 8 - timers / sleep
* Chapter 9 - `evalOn` / `blocking`
* Chapter 10 - cancelation? (this may just be too hard)

## Running the provided solutions

The solutions are built with [scala-cli](https://scala-cli.virtuslab.org). To
get you started quickly, here are some of the most common operations:

### Run tests

`scala-cli test .`

### Import into IDE

`scala-cli setup-ide .`
