package org.aiotrade.lib.io

import java.nio.channels.CancelledKeyException
import java.nio.channels.SelectableChannel
import java.nio.channels.spi.SelectorProvider
import java.util.concurrent.LinkedBlockingQueue
import scala.actors.Actor
import scala.collection.JavaConversions._


class SelectActor(ops: Int) extends Actor {

  // @Note tried using ConcurrentMap here before, but no success
  private val newListeners = new LinkedBlockingQueue[ChannelListener]

  private val selector = SelectorProvider.provider.openSelector

  def addListener(listener: ChannelListener) {
    newListeners put listener
    selector.wakeup
  }

  def stop {
    super.exit
  }

  def act = loop {
    // blocking call. selector.select call is so much like actor's receive,
    // but when wrapped in this loop, this actor lacks ability to receive message
    selector.select

    val keys = selector.selectedKeys.iterator
    while (keys.hasNext) {
      val key = keys.next
      keys.remove

      if (key.isValid) {
        import SelectActor._
        val listener = key.attachment.asInstanceOf[ChannelListener]
        val event =
          if (key.isReadable) {
            Read(this)
          } else if (key.isWritable) {
            Write(this)
          } else if (key.isConnectable) {
            Connect(this)
          } else
            Unknown(this)

        // Remove the key so we don't immediately loop around to race on the same connection.
        key.cancel

        // fire event
        listener ! event
      }
    }

    // Add queued up connections before looping back around:
    val listeners = new java.util.Vector[ChannelListener]
    newListeners.drainTo(listeners)

    for (listener <- listeners.iterator if listener.isOpen) {
      try {
        listener.channel.register(selector, ops, listener)
      } catch {case ex: CancelledKeyException => ex.printStackTrace}
    }
  }

}

trait ChannelListener extends Actor {
  def isOpen: Boolean
  def channel: SelectableChannel
}

object SelectActor {
  sealed abstract class Event(sender: SelectActor)
  final case class Read   (sender: SelectActor) extends Event(sender)
  final case class Write  (sender: SelectActor) extends Event(sender)
  final case class Connect(sender: SelectActor) extends Event(sender)
  final case class Unknown(sender: SelectActor) extends Event(sender)
}

