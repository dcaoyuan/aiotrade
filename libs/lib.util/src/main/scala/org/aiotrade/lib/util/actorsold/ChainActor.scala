/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.aiotrade.lib.util.actorsold

import scala.actors.Actor
import scala.collection.mutable.ListBuffer

object ChainActor {
  /** a private case object None that will not accessable outside here */
  private case object None
  /** noneAction will never be matched since None is private */
  private val noneAction: PartialFunction[Any, Unit] = {case None => exit}
}

trait ChainActor extends Actor {
  protected val actorActions = new ListBuffer[PartialFunction[Any, Unit]]

  def act {
    Actor.loop {
      react {
        chainReactions
      }
    }
  }

  private def chainReactions: PartialFunction[Any, Unit] =
    (ChainActor.noneAction /: actorActions)(_ orElse _)
}