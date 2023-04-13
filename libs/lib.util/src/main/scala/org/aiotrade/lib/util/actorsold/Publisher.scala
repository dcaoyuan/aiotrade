/*                     __                                               *\
 **     ________ ___   / /  ___     Scala API                            **
 **    / __/ __// _ | / /  / _ |    (c) 2007-2010, LAMP/EPFL             **
 **  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
 ** /____/\___/_/ |_/____/_/ | |                                         **
 **                          |/                                          **
 \*                                                                      */



package org.aiotrade.lib.util.actorsold

import scala.collection._
import scala.collection.mutable.HashSet
import org.aiotrade.lib.util.reactors.Reactions
import org.aiotrade.lib.util.reactors.RefSet
import org.aiotrade.lib.util.reactors.StrongReference



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
trait Publisher extends Reactor {
  
  val listeners = new RefSet[Reactor] {
    import Reactions._
    import scala.ref._
    val underlying = new HashSet[Reference[Reactor]]
    protected def Ref(a: Reactor) = a match {
      case a: StronglyReferenced => new StrongReference[Reactor](a) with super.Ref[Reactor]
      case _ => new WeakReference[Reactor](a, referenceQueue) with super.Ref[Reactor]
    }
  }
  
  private[actorsold] def subscribe(listener: Reactor)   { listeners += listener }
  private[actorsold] def unsubscribe(listener: Reactor) { listeners -= listener }
  
  /**
   * Notify all registered reactions.
   */
  def publish(e: Any) { 
    for (l <- listeners) l ! e 
  }

  listenTo(this)
}

/**
 * A publisher that subscribes itself to an underlying event source not before the first 
 * reaction is installed. Can unsubscribe itself when the last reaction is uninstalled.
 */
private[actorsold] trait LazyPublisher extends Publisher {
  import Reactions._
  
  protected def onFirstSubscribe()
  protected def onLastUnsubscribe()
  
  override def subscribe(listener: Reactor) {
    if (listeners.size == 1) onFirstSubscribe()
    super.subscribe(listener) 
  }
  override def unsubscribe(listener: Reactor) {
    super.unsubscribe(listener) 
    if (listeners.size == 1) onLastUnsubscribe()
  }
}



