package org.aiotrade.lib.io

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.channels.SelectionKey
import java.nio.channels.ServerSocketChannel
import java.nio.channels.spi.SelectorProvider
import scala.actors.Actor
import Encoding._


class FileReceiver(hostAddress: InetAddress, port: Int, storageDirPath: String) extends Actor {

  val storageDir = new File(storageDirPath)
  if (!storageDir.exists) {
    storageDir.mkdirs
  }

  // Create a new non-blocking server socket channel
  val serverChannel = ServerSocketChannel.open
  serverChannel.configureBlocking(true)

  // Bind the server socket to the specified address and port
  serverChannel.socket.bind(new InetSocketAddress(hostAddress, port))

  val selector = SelectorProvider.provider.openSelector

  val selectorActor = new SelectDispatcher(selector)
  val selectReactor = new SelectReactor(selectorActor)
  selectReactor.start

  selectorActor.addListener(selectReactor)
  selectorActor.start

  def act = loop {
    val clientChannel = serverChannel.accept
    if (clientChannel != null) {
      clientChannel.configureBlocking(false)
      println("new connection accepted")

      val handler = new FileReceiver.FileReceiverHandler(storageDir)
      handler.start
      selectReactor ! SetResponseHandler(clientChannel, Some(handler))

      // Register the new channel with our selector via selectorActor, interested in data waiting to be read
      selectorActor.requestChange(InterestInOps(clientChannel, SelectionKey.OP_READ))
    }
  }
}


object FileReceiver {

  // ----- simple test
  def main(args: Array[String]) {
    try {
      new FileReceiver(null, 4711, System.getProperty("user.home") + File.separator + "storage") start
    } catch {case ex: IOException => ex.printStackTrace}
  }

  class FileReceiverHandler(storageDir: File) extends Actor {

    private val requestParser = new RequestParser(storageDir)

    private def handleRequest(data: Array[Byte]) = {
      var i = 0
      while (i < data.length) {
        requestParser.consume(data(i))
        i += 1
      }

      requestParser.parsed
    }

    def act = loop {
      react {
        case ProcessData(reactor, channel, key, data) =>
          val finished = handleRequest(data)
          // The handler has seen enough?, if true, close the connection
//        if (finished) {
//          channel.close
//          key.cancel
//        }
      }
    }

    /**
     * A continuation-based request parser.
     *
     * The parser consumes one character at a time,
     * which means that the parsing process can be suspended at any time.
     *
     * At the moment, this does not support Keep-Alive connections.
     */
    private class RequestParser(storageDir: File) {

      /**
       * Indicates the current position of the parser.
       */
      sealed trait State
      case class NumFiles(buf: Array[Byte], idx: Int, len: Int) extends State
      case class FileMeta(name: Option[String], buf: Array[Byte], idx: Int, len: Int) extends State
      case class FileData(out: OutputStream, idx: Long, len: Long) extends State
      case object End extends State
      val NoneName = Some("")

      private var state: State = NumFiles(new Array[Byte](4), 0, 4)

      /* Components of the request. */
      private var numFiles = 0
      private var cntFiles = 0

      /**
       * Has a complete request been parsed?
       */
      def parsed = state == End

      /**
       * Update the state of the parser with the next character.
       */
      def consume(b: Byte) {
        state = state match {
          case NumFiles(buf, i, len) if i < len - 1 => buf(i) = b; NumFiles(buf, i + 1, len)
          case NumFiles(buf, i, _) =>
            buf(i) = b
            numFiles = decodeInt(buf)
            //println("number of files: " + numFiles)
            FileMeta(None, new Array[Byte](4), 0, 4)

            // read filename length
          case FileMeta(None, buf, i, len) if i < len - 1 => buf(i) = b; FileMeta(None, buf, i + 1, len)
          case FileMeta(None, buf, i, _) =>
            buf(i) = b
            val len = decodeInt(buf)
            //println("file name length: " + len)
            FileMeta(NoneName, new Array[Byte](len), 0, len)

            // read filename string
          case FileMeta(NoneName, buf, i, len) if i < len - 1 => buf(i) = b; FileMeta(NoneName, buf, i + 1, len)
          case FileMeta(NoneName, buf, i, _) =>
            buf(i) = b
            val path = new String(buf) + "-" + System.currentTimeMillis
            //println("file path: " + path)
            // expect file length in Long
            FileMeta(Some(path), new Array[Byte](8), 0, 8)

            // read file length
          case FileMeta(Some(path), buf, i, len) if i < len - 1 => buf(i) = b; FileMeta(Some(path), buf, i + 1, len)
          case FileMeta(Some(path), buf, i, _) =>
            buf(i) = b
            val len = decodeLong(buf)
            //println("file length: " + len)

            val file = new File(path)
            val fileName = file.getName
            val saveToFile = new File(storageDir, fileName)
            val out = new FileOutputStream(saveToFile)
            FileData(out, 0, len)

            // read file content
          case FileData(out, i, len) if i < len - 1 => out.write(b); FileData(out, i + 1, len)
          case FileData(out, _, _) =>
            out.write(b)
            out.close
            cntFiles += 1
            // all files received? if true end, else begin a new file
            if (cntFiles == numFiles) End else FileMeta(None, new Array[Byte](4), 0, 4)

          case _ => throw new Exception(state + "/" + b)
        }
      }
    }
  }

}
