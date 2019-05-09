package com.outr.solr4s.name

import org.powerscala.io.IO

object Aliases {
  private var map = Map.empty[String, Set[String]]

  rebuild()

  def rebuild(): Unit = synchronized {
    val lines = IO.stream(getClass.getClassLoader.getResourceAsStream("names.csv"), new StringBuilder).toString.split('\n')
    map = Map.empty
    lines.foreach { line =>
      val names = line.split(',').map(_.trim).toSet
      names.foreach { name =>
        val current = apply(name)
        map += name -> (current ++ names)
      }
    }
  }

  def apply(name: String, excludeOriginal: Boolean = false): Set[String] = {
    val lower = name.toLowerCase
    val set = map.getOrElse(lower, Set(lower))
    if (excludeOriginal) {
      set - lower
    } else {
      set
    }
  }
}