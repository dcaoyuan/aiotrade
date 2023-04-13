/*                     __                                               *\
 **     ________ ___   / /  ___     Scala API                            **
 **    / __/ __// _ | / /  / _ |    (c) 2007-2010, LAMP/EPFL             **
 **  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
 ** /____/\___/_/ |_/____/_/ | |                                         **
 **                          |/                                          **
 \*                                                                      */



package org.aiotrade.lib.util.reactors

import java.util.logging.Logger
import java.util.logging.Level
import scala.collection.mutable.{Buffer, ListBuffer}

object Reactions {
  import scala.ref._

  final class Impl extends Reactions {
    val log = Logger.getLogger(getClass.getName)
    private val parts: Buffer[Reaction] = new ListBuffer[Reaction]
    def isDefinedAt(e: Any) = parts.exists(_ isDefinedAt e)
    def += (r: Reaction): this.type = { parts += r; this }
    def -= (r: Reaction): this.type = { parts -= r; this }
    def apply(e: Any) {
      try {
        for (p <- parts if p isDefinedAt e) p(e)
      } catch {
        case ex: Throwable => log.log(Level.SEVERE, ex.getMessage, ex)
      }
    }
  }

  type Reaction = PartialFunction[Any, Unit]

  /**
   * A Reaction implementing this trait is strongly referenced in the reaction list
   */
  trait StronglyReferenced

  final class Wrapper(listener: Any)(r: Reaction) extends Reaction with StronglyReferenced with Proxy {
    def self = listener
    def isDefinedAt(e: Any) = r.isDefinedAt(e)
    def apply(e: Any) { r(e) }
  }
}

/**
 * Used by reactors to let clients register custom event reactions.
 */
abstract class Reactions extends Reactions.Reaction {
  /**
   * Add a reaction.
   */
  def += (r: Reactions.Reaction): this.type

  /**
   * Remove the given reaction.
   */
  def -= (r: Reactions.Reaction): this.type
}