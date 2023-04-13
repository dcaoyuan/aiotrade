package org.aiotrade.lib.io


import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.SocketChannel
import scala.actors.Actor
import scala.collection.immutable.Queue
import scala.collection.mutable

final case class ProcessData(reactor: Actor, socket: SocketChannel, key: SelectionKey, data: Array[Byte])
final case class SendData(channel: SocketChannel, data: ByteBuffer, rspHandler: Option[Actor])
final case class SetResponseHandler(channel: SocketChannel, rspHandler: Option[Actor])

class SelectReactor(dispatcher: SelectDispatcher) extends Actor {
  // The buffer into which we'll read data when it's available
  private val readBuffer = ByteBuffer.allocate(8192)

  private val pendingData = mutable.Map[SocketChannel, Queue[ByteBuffer]]()

  // Maps a SocketChannel to a Handler
  private val rspHandlers = mutable.Map[SocketChannel, Actor]()

  def act = loop {
    react {
      case SetResponseHandler(channel, rspHandler) =>
        // Register the response handler
        rspHandler foreach {x => rspHandlers += (channel -> x)}

      case SendData(channel, data, rspHandler) =>
        // Register the response handler
        rspHandler foreach {x => rspHandlers += (channel -> x)}

        // And queue the data we want written
        val queue = pendingData.get(channel) match {
          case None => Queue(data)
          case Some(x) => x enqueue data
        }
        pendingData += (channel -> queue)

        // Fianally, indicate we want the interest ops set changed
        dispatcher.requestChange(InterestInOps(channel, SelectionKey.OP_WRITE))

      case ConnectKey(key) => // not used yet
        // Register an interest in writing on this channel
        key.interestOps(SelectionKey.OP_WRITE)

      case ReadKey(key) => read(key)
      case WriteKey(key) => write(key)
    }
  }

  @throws(classOf[IOException])
  private def read(key: SelectionKey) {
    val socketChannel = key.channel.asInstanceOf[SocketChannel]

    // Clear out our read buffer so it's ready for new data
    readBuffer.clear

    // Attempt to read off the channel
    var numRead = -1
    try {
      numRead = socketChannel.read(readBuffer)
    } catch {case ex: IOException =>
        // The remote forcibly closed the connection, cancel
        // the selection key and close the channel.
        key.cancel
        socketChannel.close
        return
    }

    if (numRead == -1) {
      // Remote entity shut the socket down cleanly. Do the
      // same from our end and cancel the channel.
      key.cancel
      socketChannel.close
      return
    }

    if (numRead > 0) {
      // Look up the handler for this channel
      rspHandlers.get(socketChannel) foreach {handler =>
        // Make a correctly sized copy of the data before handing it to the client
        val data = new Array[Byte](numRead)
        System.arraycopy(readBuffer.array, 0, data, 0, numRead)
        // Hand the data off to our handler actor
        handler ! ProcessData(this, socketChannel, key, data)
      }
    }
  }

  @throws(classOf[IOException])
  private def write(key: SelectionKey) {
    val socketChannel = key.channel.asInstanceOf[SocketChannel]

    var queue = pendingData.get(socketChannel).getOrElse(return)

    // Write until there's not more data ...
    var done = false
    while (!queue.isEmpty && !done) {
      val (head, tail) = queue.dequeue
      socketChannel.write(head)
      if (head.remaining > 0) {
        // ... or the socket's buffer fills up
        done = true
      } else {
        queue = tail
      }
    }
    pendingData(socketChannel) = queue

    if (queue.isEmpty) {
      // We wrote away all data, so we're no longer interested
      // in writing on this socket. Switch back to waiting for
      // data.
      key.interestOps(SelectionKey.OP_READ)
    }
  }
}