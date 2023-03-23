package io.github.timwspence.byoes
package unsafe

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext

object implicits:
  given IORuntime = IORuntime(ExecutionContext.global)
