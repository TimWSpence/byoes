# Chapter 2

There is a good chance that the `unsafeRunSync` implementation from chapter 1 was not stacksafe. Now is the time to fix this!

We want to make sure a test like this passes:

```scala
test("stack safe") {

  def go(n: Int): IO[Unit] = if (n == 1) IO.unit else IO.unit.flatMap(_ => go(n-1))

  go(20000).unsafeRunSync()

}
```
