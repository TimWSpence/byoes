# Hints

We now have two kinds of continuation in play - `flatMap` and
`handleErrorWith`. So you will need to modify your contintuation stack to track
which kind each continuation is and unwind the stack whenever you raise an
error.
