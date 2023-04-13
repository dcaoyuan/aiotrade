package org.aiotrade.lib.io

import java.io.IOException
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.SocketChannel
import scala.actors.Actor

final case class InterestInOps(channel: SocketChannel, ops: Int)

class SelectDispatcher(selector: Selector) extends Actor {

  private var listeners = List[Actor]()
  private var pendingChanges = List[InterestInOps]()

  def addListener(listener: Actor) {
    listeners synchronized {listeners ::= listener}
  }

  def requestChange(change: InterestInOps) {
    pendingChanges synchronized {pendingChanges ::= change}

    // wake up selecting thread so it can make the required changes.
    // which will interrupt selector.select blocking, so the loop can re-begin
    selector.wakeup
  }

  def act = loop {
    try {
      pendingChanges synchronized {
        pendingChanges foreach {
          case InterestInOps(channel, ops) =>
            val key = channel.keyFor(selector)
            if (key == null) {
              channel.register(selector, ops)
            } else {
              key.interestOps(ops)
            }
        }

        pendingChanges = Nil
      }

      selector.select

      // Iterate over the set of keys for which events are available
      val selectedKeys = selector.selectedKeys.iterator
      while (selectedKeys.hasNext) {
        val key = selectedKeys.next
        selectedKeys.remove

        if (key.isValid) {
          // Check what event is available and deal with it
          if (key.isAcceptable) {
            // it seems OP_ACCEPT can not be caught in actor's loop method, but
          } else  if (key.isConnectable) {
            // it seems OP_CONNECT can not be caught in actor's loop method, but
            // still keep the code here
            if (finishConnection(key)) {
              listeners foreach {_ ! ConnectKey(key)}
            }
          } else if (key.isReadable) {
            listeners foreach {_ ! ReadKey(key)}
          } else if (key.isWritable) {
            listeners foreach {_ ! WriteKey(key)}
          }
        }
      }
    } catch {
      case ex: Exception => ex.printStackTrace
    }
  }

  /**
   * When a connectable key selected, finishConnect call should be in same thread,
   * so we finishConnection here
   */
  @throws(classOf[IOException])
  private def finishConnection(key: SelectionKey): Boolean = {
    val socketChannel = key.channel.asInstanceOf[SocketChannel]

    // Finish the connection. If the connection operation failed
    // this will raise an IOException.
    try {
      socketChannel.finishConnect
    } catch {
      case ex: IOException =>
        // Cancel the channel's registration with our selector
        ex.printStackTrace
        key.cancel
        false
    }

    true
  }
}

