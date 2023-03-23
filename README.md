# Chapter 4

Up until now, we have been evaluating `IO` actions on the thread calling
`unsafeRunSync`. This will become problematic when we come to implement fiber
forking - we are effectively creating a 1-1 correspondence between fibers and
threads. Rather, we would like our `IO` actions to be submitted to a threadpool
so that we an schedule many `IO` actions (fibers) on a fixed size threadpool
The high-level design we are ultimately aiming towards is described in great
detail [here](https://tokio.rs/blog/2019-10-scheduler) and at least a brief
skim of that now is probably beneficial.

The goal of this chapter is to introduce a fixed size threadpool (we will
customize this in the future but for now you can use
`Executors.fixedSizeThreadpool`) and have `IO` actions be submitted to it. This
means that `IO` actions are now evaluated asynchronously, so you may find it
useful to introduce a `def unsafeToFuture()` method on `IO` and have
`unsafeRunSync()` block on the completion of that future.

The design space is wide open here but the gently suggested approach is to
require an implicit runtime in order to invoke `unsafeRunSync` and
`unsafeToFuture`. This runtime can hold a reference to the threadpool that the
action is to be submitted to.
