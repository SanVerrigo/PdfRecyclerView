package com.verrigo.pdfviewer

import kotlin.math.min
import kotlin.math.max

internal fun IntRange.findIntersection(range: IntRange): IntRange {
  val start = max(first, range.first)
  val end = min(last, range.last)
  return if (start > end) {
    IntRange.EMPTY
  } else {
    IntRange(start, end)
  }
}

internal fun <T> Iterable<T>.size(): Int {
  if (this is Collection<*>) {
    return this.size
  }
  var i = 0
  for (obj in this) i++
  return i
}

