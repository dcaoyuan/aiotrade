package org.aiotrade.lib.io

import java.io.IOException
import java.io.RandomAccessFile
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.channels.SocketChannel
import java.nio.channels.spi.SelectorProvider
import Encoding._


/**
 * @param host host of receiver
 * @param port port of receiver
 */
class FileSender(hostAddress: String, port: Int) {

  val selector = SelectorProvider.provider.openSelector

  val selectDispatcher = new SelectDispatcher(selector)
  val selectReactor = new SelectReactor(selectDispatcher)
  selectReactor.start

  selectDispatcher.addListener(selectReactor)
  selectDispatcher.start

  @throws(classOf[IOException])
  def initiateConnection: SocketChannel = {
    // open an channel and kick off connecting
    val socketChannel = SocketChannel.open
    socketChannel.connect(new InetSocketAddress(hostAddress, port))

    /**
     * @Note actor's loop is not compitable with non-blocking mode, i.e. cannot work with SelectionKey.OP_CONNECT
     */
    // Finish the connection. If the connection operation failed this will raise an IOException.
    try {
      while (!socketChannel.finishConnect) {}
    } catch {case ex: IOException =>
        ex.printStackTrace
        return null
    }

    // then we can set it non-blocking
    socketChannel.configureBlocking(false)

    socketChannel
  }

  @throws(classOf[IOException])
  def send(files: Array[String]) {
    try {
      val channel = initiateConnection
      
      sendInt(channel, files.length)
      for (file <- files) {
        sendString(channel, file)
        sendFile(channel, file)
      }
    } catch {case ex: Exception =>
        ex.printStackTrace
        throw new IOException("Failed to send")
    }
  }

  private def sendInt(channel: SocketChannel, i: Int) {
    val bytes = encodeInt(i)
    selectReactor ! SendData(channel, ByteBuffer.wrap(bytes), None)
  }

  private def sendLong(channel: SocketChannel, i: Long) {
    val bytes = encodeLong(i)
    selectReactor ! SendData(channel, ByteBuffer.wrap(bytes), None)
  }

  private def sendString(channel: SocketChannel, s: String) {
    val bytes = s.getBytes
    val len = bytes.length
    sendInt(channel, len)
    selectReactor ! SendData(channel, ByteBuffer.wrap(bytes), None)
  }

  private def sendFile(channel: SocketChannel, file: String) {
    val fileChannel = (new RandomAccessFile(file, "r")).getChannel
    val size = fileChannel.size
    val byteBuf = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, size)
    sendLong(channel, size)
    selectReactor ! SendData(channel, byteBuf, None)
  }

}

object FileSender {

  // ----- simple test
  def main(files: Array[String]) {
    val files = Array("pom.xml", "pom.xml")
    val sender = new FileSender("localhost", 4711)
    sender.send(files)
    sender.send(files)
  }
}

