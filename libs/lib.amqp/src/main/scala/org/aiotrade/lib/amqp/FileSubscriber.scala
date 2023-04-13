/*
 * Copyright (c) 2006-2011, AIOTrade Computing Co. and Contributors
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 *  o Redistributions of source code must retain the above copyright notice, 
 *    this list of conditions and the following disclaimer. 
 *    
 *  o Redistributions in binary form must reproduce the above copyright notice, 
 *    this list of conditions and the following disclaimer in the documentation 
 *    and/or other materials provided with the distribution. 
 *    
 *  o Neither the name of AIOTrade Computing Co. nor the names of 
 *    its contributors may be used to endorse or promote products derived 
 *    from this software without specific prior written permission. 
 *    
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.aiotrade.lib.amqp

import com.rabbitmq.client.ConnectionFactory
import java.io.File
import java.io.FileOutputStream
import java.util.logging.Logger

object FileSubscriber {

  // --- simple test
  def main(args: Array[String]) {
    val host = "localhost"
    val port = 5672

    val queue = "filequeue"
    val exchange = "market.internal"
    val routingKey = "source.file.cndbf"

    val outputDirPath = System.getProperty("user.home") + File.separator + "storage"

    val factory = new ConnectionFactory
    factory.setHost(host)
    factory.setPort(port)
    factory.setUsername("guest")
    factory.setPassword("guest")
    factory.setVirtualHost("/")
    factory.setRequestedHeartbeat(0)

    for (i <- 0 until 5) {
      val queuei = queue + i
      val subscriber = new FileSubscriber(factory,
                                          exchange,
                                          outputDirPath)
      
      new subscriber.SafeProcessor
      subscriber.connect
      subscriber.consumeQueue(queuei, true, false, false, true)
      subscriber.subscribeTopic(routingKey)
    }
  }

}

class FileSubscriber(factory: ConnectionFactory, exchange: String, outputDirPath: String, isAutoAck: Boolean = true
) extends AMQPSubscriber(factory, exchange) {
  val outputDir = new File(outputDirPath)
  if (!outputDir.exists) {
    outputDir.mkdirs
  } else {
    assert(outputDir.isDirectory, "outputDir should be director: " + outputDir)
  }


  class DefaultProcessor extends Processor {
    def process(msg: AMQPMessage) {
      val headers = msg.props.getHeaders
      val body = msg.body.asInstanceOf[Array[Byte]]

      var fileName = headers.get("filename").toString
      var outputFile = new File(outputDir, fileName)
      var i = 1
      while (outputFile.exists) {
        fileName = fileName + "_" + i
        outputFile = new File(outputDir, fileName)
        i += 1
      }
        
      val out = new FileOutputStream(outputFile)
      out.write(body)
      out.close
    }
  }

  /**
   * Firstly save the file with a temporary file name.
   * When finish receiving all the data, then rename to the regular file in the same folder.
   */
  class SafeProcessor extends Processor {
    private val log = Logger.getLogger(this.getClass.getName)
    
    def process(msg: AMQPMessage) {
      val headers = msg.props.getHeaders
      val body = msg.body.asInstanceOf[Array[Byte]]

      var fileName = headers.get("filename").toString
      var outputFile = new File(outputDir, "." + fileName + ".tmp")
      var i = 1
      while (outputFile.exists) {
        fileName = fileName + "_" + i
        outputFile = new File(outputDir, "." + fileName + ".tmp")
        i += 1
      }
        
      val out = new FileOutputStream(outputFile)
      out.write(body)
      out.close

      outputFile.renameTo(new File(outputDir, fileName))
      log.info("Received " + fileName)
    }
  }

}
