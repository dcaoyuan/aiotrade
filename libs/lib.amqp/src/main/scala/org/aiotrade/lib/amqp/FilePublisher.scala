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

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.AMQP.BasicProperties
import com.rabbitmq.client.Consumer
import java.io.File
import java.io.FileInputStream
import java.io.IOException

object FilePublisher {

  // --- simple test
  def main(args: Array[String]) {
    val nSubscribers = 5

    val host = "localhost"
    val port = 5672

    val exchange = "market.internal"
    val routingKey = "source.file.cndbf"

    val factory = new ConnectionFactory
    factory.setHost(host)
    factory.setPort(port)
    factory.setUsername("guest")
    factory.setPassword("guest")
    factory.setVirtualHost("/")
    factory.setRequestedHeartbeat(0)

    val publisher = new FilePublisher(factory, exchange, routingKey, true)
    publisher.connect
    val files = List(new File("pom.xml"), new File("src/test/resources/testfile.txt"))

    publisher.sendFiles(files)
    System.exit(0)
  }
}

class FilePublisher(factory: ConnectionFactory, exchange: String, routingKey: String, durable: Boolean = false
) extends AMQPDispatcher(factory, exchange) {

  @throws(classOf[IOException])
  def configure(channel: Channel): Option[Consumer] = {
    channel.exchangeDeclare(exchange, "direct", durable)
    None
  }

  @throws(classOf[IOException])
  def sendFiles(files: List[File]) {
    files foreach (sendFile(_))
  }

  @throws(classOf[IOException])
  def sendFile(file: File, toName: Option[String] = None) {
    val is = new FileInputStream(file)
    val length = file.length.toInt
    val body = new Array[Byte](length)
    is.read(body)
    is.close

    sendFile(body, toName.getOrElse(file.getName))
  }

  def sendFile(body: Array[Byte], toName: String) {
    val headers: java.util.Map[String, AnyRef] = new java.util.HashMap
    headers.put("filename", toName)
    headers.put("length", body.length.asInstanceOf[AnyRef])
    val propsbd = new BasicProperties.Builder().headers(headers).contentType(ContentType.OCTET_STREAM.mimeType)
    if (durable) propsbd.deliveryMode(2) // persistent
    publish(body, exchange, routingKey, propsbd.build)
  }

}
