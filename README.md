# Chapter 5

Now that all our fibers are running on a fixed-size threadpool (although we
have no convenient way of forking new fibers yet) they are competing for CPU
time. In our current implementation, once a fiber is scheduled it will run to
completion and any other pending fibers will have to wait. This is a major
problem for any kind of responsive system.

The goal of this chapter is to implement `IO.cede`, which is a signal to the
runtime that the current fiber should be de-scheduled to allow others to run.
In addition, we will modify the runloop so that `cede`s are automatically
inserted every `n` iterations (for some constant `n`).
