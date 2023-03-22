# Hints

There are multiple approaches to stack safety. One famous technique is the
[trampoline approach](https://blog.higher-order.com/assets/trampolines.pdf).
This does however require allocating lots of intermediate closures (see
[here](https://discord.com/channels/632277896739946517/839263556754472990/1079778226248884294)
for discussion) so is not particularly performant on the JVM.

Cats Effect (and the solution here) adopt a different strategy based on storing
a continuation stack on the heap.
