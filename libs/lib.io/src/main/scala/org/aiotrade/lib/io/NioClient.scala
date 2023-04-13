package org.aiotrade.lib.io

import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import java.nio.channels.spi.SelectorProvider
import scala.actors.Actor

/**
 * @parem hostAddress the host to connect to
 * @param port the port to connect to
 */
@throws(classOf[IOException])
class NioClient(hostAddress: InetAddress, port: Int) {

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

    // Register an interest in writing on this channel
    //rwSelector.requestChange(Register(socketChannel, SelectionKey.OP_CONNECT))

    socketChannel
  }

}

object NioClient {
  // ----- simple test
  def main(args: Array[String]) {
    try {
      val client = new NioClient(InetAddress.getByName("localhost"), 9090)
      client.selectReactor.start
      
      val handler = new EchoRespHandler
      handler.start

      val channel = client.initiateConnection
      client.selectReactor ! SendData(channel, ByteBuffer.wrap("Hello World".getBytes), Some(handler))
    } catch {case ex: Exception => ex.printStackTrace}
  }

  class EchoRespHandler extends Actor {

    def handleResponse(resp: Array[Byte]): Boolean = {
      println(new String(resp))
      true
    }

    def act = loop {
      react {
        case ProcessData(reactor, channel, key, data) =>
          val finished = handleResponse(data)
          // The handler has seen enough?, if true, close the connection
          if (finished) {
            channel.close
            key.cancel
          }
      }
    }
  }
}
