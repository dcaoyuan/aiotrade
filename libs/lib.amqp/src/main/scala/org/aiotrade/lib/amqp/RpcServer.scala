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

import com.rabbitmq.client.Channel
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.Consumer
import java.io.IOException
import java.util.logging.Level
import java.util.logging.Logger

object RpcServer {
  private val log = Logger.getLogger(this.getClass.getName)

  def declareServer(factory: ConnectionFactory, exchange: String, requestQueues: Seq[String]) {
    try {
      val conn = factory.newConnection
      val channel = conn.createChannel
      
      if (exchange != AMQPExchange.defaultDirect) channel.exchangeDeclare(exchange, "direct")

      declareQueue(channel, exchange, requestQueues)

      conn.close
    } catch {
      case ex: Throwable => log.log(Level.SEVERE, ex.getMessage, ex)
    }
  }

  /**
   * @Note queue should always be declared with same props in object RpcServer and in class RpcServer
   */
  private def declareQueue(channel: Channel, exchange: String, requestQueues: Seq[String]) {
    for (requestQueue <- requestQueues) {
      // durable = false, exclusive = false, autoDelete = true
      channel.queueDeclare(requestQueue, false, false, true, null)

      // use routingKey identical to queue name
      val routingKey = requestQueue
      channel.queueBind(requestQueue, exchange, routingKey)
    }
  }
}

/**
 * Class which manages a request queue for a simple RPC-style service.
 * The class is agnostic about the format of RPC arguments / return values.
 * @param Channel we are communicating on
 * @param Queue to receive requests from
 */
class RpcServer($factory: ConnectionFactory, $exchange: String, val requestQueue: String
) extends AMQPDispatcher($factory, $exchange) {
  assert(requestQueue != null && requestQueue != "", "We need explicitly named requestQueue")

  /**
   * Creates an RpcServer listening on a temporary exclusive
   * autodelete queue.
   */
  @throws(classOf[IOException])
  def this(factory: ConnectionFactory) = this(factory, "", null)

  @throws(classOf[IOException])
  override def configure(channel: Channel): Option[Consumer] = {
    // Set prefetchCount to 1, so the requestQueue can be shared and balanced by lots of rpc servers on 1 message each time behavior
    channel.basicQos(1)

    if (exchange != AMQPExchange.defaultDirect) channel.exchangeDeclare(exchange, "direct")

    RpcServer.declareQueue(channel, exchange, List(requestQueue))

    val consumer = new AMQPConsumer(channel, false)
    channel.basicConsume(requestQueue, consumer.isAutoAck, consumer)
    Some(consumer)
  }

  /**
   * Processor that will automatically added as listener of this AMQPDispatcher
   * and process AMQPMessage and reply to client via process(msg).
   */
  abstract class Handler extends Processor {

    /**
     * @return AMQPMessage that will be send back to caller
     */
    def process(amqpMsg: AMQPMessage) {
      amqpMsg match {
        case AMQPMessage(req, reqProps, _)  =>
          val correlationId = reqProps.getCorrelationId
          val replyTo = reqProps.getReplyTo
          if (correlationId != null && replyTo != null) {
            handle(req) match {
              case AMQPMessage(body, props, env) =>
                val replyProps = props.builder
                .contentType(reqProps.getContentType)
                .contentEncoding(reqProps.getContentEncoding)
                .correlationId(correlationId)
                .build
                
                publish(body, "", replyTo, replyProps)
            }
          }
      }
    }
    
    /**
     * @return AMQPMessage that will be send back to caller
     */
    protected def handle(req: Any): AMQPMessage
  }
}