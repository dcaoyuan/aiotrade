/*                     __                                               *\
 **     ________ ___   / /  ___     Scala API                            **
 **    / __/ __// _ | / /  / _ |    (c) 2007-2010, LAMP/EPFL             **
 **  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
 ** /____/\___/_/ |_/____/_/ | |                                         **
 **                          |/                                          **
 \*                                                                      */



package org.aiotrade.lib.util.reactors

import scala.collection._
import scala.collection.mutable.HashSet

/** <p>
 *    Notifies registered reactions when an event is published. Publishers are
 *    also reactors and listen to themselves per default as a convenience.
 *  </p>
 *  <p>
 *    In order to reduce memory leaks, reactions are weakly referenced by default,
 *    unless they implement <code>Reactions.StronglyReferenced</code>. That way,
 *    the lifetime of reactions are more easily bound to the registering object,
 *    which are reactors in common client code and hold strong references to their
 *    reactions. As a result, reactors can be garbage collected even though they
 *    still have reactions registered at some publisher, but not vice versa
 *    since reactors (strongly) reference publishers they are interested in.
 *  </p>
 */
@deprecated("Use actors.Reactor")
trait Publisher extends Reactor {
  import Reactions._

  protected val listeners = new RefSet[Reaction] {
    import scala.ref._
    val underlying = new HashSet[Reference[Reaction]]
    protected def Ref(a: Reaction) = a match {
      case a: StronglyReferenced => new StrongReference[Reaction](a) with super.Ref[Reaction]
      case _ => new WeakReference[Reaction](a, referenceQueue) with super.Ref[Reaction]
    }
  }

  private[reactors] def subscribe(listener: Reaction) { listeners += listener }
  private[reactors] def unsubscribe(listener: Reaction) { listeners -= listener }

  /**
   * Notify all registered reactions.
   */
  def publish(e: Any) { for (l <- listeners) l(e) }

  listenTo(this)
}

/**
 * A publisher that subscribes itself to an underlying event source not before the first
 * reaction is installed. Can unsubscribe itself when the last reaction is uninstalled.
 */
private[reactors] trait LazyPublisher extends Publisher {
  import Reactions._

  protected def onFirstSubscribe()
  protected def onLastUnsubscribe()

  override def subscribe(listener: Reaction) {
    if(listeners.size == 1) onFirstSubscribe()
    super.subscribe(listener)
  }
  override def unsubscribe(listener: Reaction) {
    super.unsubscribe(listener)
    if(listeners.size == 1) onLastUnsubscribe()
  }
}



