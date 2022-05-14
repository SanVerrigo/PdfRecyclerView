package com.gemarktech.x5group

import kotlin.math.min
import kotlin.math.max

fun IntRange.findIntersection(range: IntRange): IntRange {
  val start = max(first, range.first)
  val end = min(last, range.last)
  return if (start > end) {
    IntRange.EMPTY
  } else {
    IntRange(start, end)
  }
}

