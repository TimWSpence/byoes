# Hints

## Datatypes and interpreters

`IO` is a "free" construction so your starting point is to define an ADT with a
branch for each primitive `IO` operation.

You will then want to define an "interpreter" (`unsafeRunSync`) that
pattern-matches on this construction.
