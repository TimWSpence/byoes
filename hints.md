# Hints

You will probably need to introduce an `IOFiber extends Runnable` which will
encapsulate the state of the running `IO` action. The recursive pattern-matching
that previously lived in `IO#unsafeRunSync()` will move to the `IOFiber#run`
method.  As the execution of fibers is now asynchronous, we will also need a way
to report  the result of the `IO` action to the callsite. The simplest way is to
pass a `cb: Outcome[A] => Unit` to the `IOFiber` constructor. It can then invoke
this callback when it has finished running the `IO` action.
