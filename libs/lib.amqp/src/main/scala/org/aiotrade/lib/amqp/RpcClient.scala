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
import com.rabbitmq.client.Consumer
import com.rabbitmq.client.Envelope
import com.rabbitmq.client.ShutdownSignalException
import java.io.EOFException
import java.io.IOException
import org.aiotrade.lib.avro.Evt
import scala.collection.mutable
import scala.concurrent.SyncVar
import java.util.logging.Logger

/**
 * Convenience class which manages a temporary reply queue for simple RPC-style communication.
 * The class is agnostic about the format of RPC arguments / return values.
 * It simply provides a mechanism for sending a message to an exchange with a given routing key,
 * and waiting for a response on a reply queue.
 *
 * @param channel the channel to use for communication
 * @param exchange the exchange to connect to
 * @param routingKey the routing key
 * @throws IOException if an error is encountered
 * @see #setupReplyQueue
 */
@throws(classOf[IOException])
class RpcClient($factory: ConnectionFactory, $reqExchange: String) extends AMQPDispatcher($factory, $reqExchange) {

  private val log = Logger.getLogger(getClass.getName)

  var replyQueue: String = _ // The name of our private reply queue

  /** Map from request correlation ID to continuation BlockingCell */
  private val continuationMap = mutable.Map[String, SyncVar[Any]]()
  /** Contains the most recently-used request correlation ID */
  private var correlationId = 0L
  /** Should hold strong ref for SyncVarSetterProcessor */
//  private val processor = new SyncVarSetterProcessor

  /**
   * Remove the strong holder of ref.
   * If the connection closed or shutdown, the connection can not connected again, must create a new connection.
   * And the old connection must be collected by GC.
   */
//  this.processors -= processor
  this.deafTo(this)


  @throws(classOf[IOException])
  def configure(channel: Channel): Option[Consumer] = {
    replyQueue = setupReplyQueue(channel)

    val consumer = new AMQPConsumer(channel, true) {
      override def handleShutdownSignal(consumerTag: String, signal: ShutdownSignalException) {
        continuationMap synchronized {
          for ((_, syncVar) <- continuationMap) {
            syncVar.set(signal.getMessage)
          }
        }
      }
    }

    // autoAck - true  if the server should consider messages acknowledged once delivered;
    //           false if the server should expect explicit acknowledgements
    // When consumer.isAutoAck == true, AMQPConsumer will call channel.basicAck(env.getDeliveryTag, false)
    channel.basicConsume(replyQueue, consumer.isAutoAck, consumer)
    Some(consumer)
  }

  /**
   * Creates a server-named exclusive autodelete queue to use for
   * receiving replies to RPC requests.
   * @throws IOException if an error is encountered
   * @return the name of the reply queue
   */
  @throws(classOf[IOException])
  private def setupReplyQueue(channel: Channel): String = {
    val queueName = channel.queueDeclare("", false, true, true, null).getQueue
    log.fine("declared queue " + queueName)
    queueName
  }

  /**
   * Private API - ensures the RpcClient is correctly open.
   * @throws IOException if an error is encountered
   */
  @throws(classOf[IOException])
  protected def checkConsumer {
    if (consumer.isEmpty) throw new EOFException("Consumer of rpcClient is closed")
  }

  /**
   * Perform a simple byte-array-based RPC roundtrip.
   * @param req the rpc request message to send
   * @param routingKey the rpc routingKey to publish
   * @param props for request message, default null
   * @param timeout in milliseconds, default infinit (-1)
   * @return the response received
   * @throws ShutdownSignalException if the connection dies during our wait
   * @throws IOException if an error is encountered
   */
  @throws(classOf[IOException])
  @throws(classOf[ShutdownSignalException])
  def rpc(req: Any, routingKey: String, props: AMQP.BasicProperties = new AMQP.BasicProperties.Builder().build, timeout: Long = -1): Any = {
    val syncVar = asyncRpc(req, routingKey, props)

    val res = if (timeout < 0) {
      syncVar.get
    } else {
      syncVar.get(timeout) getOrElse Evt.Error("Rpc timeout")
    }

    res
  }

  @throws(classOf[IOException])
  @throws(classOf[ShutdownSignalException])
  def arpc(req: Any, routingKey: String, props: AMQP.BasicProperties = new AMQP.BasicProperties.Builder().build, timeout: Long = -1)(action: Any => Unit) {
    val syncVar = asyncRpc(req, routingKey, props)

    scala.actors.Actor.actor {
      val res = if (timeout < 0) {
        syncVar.get
      } else {
        syncVar.get(timeout) getOrElse Evt.Error("Rpc timeout")
      }

      action(res)
    }
  }

  /**
   * Perform a async simple byte-array-based RPC roundtrip.
   * @param req the rpc request message to send
   * @param routingKey the rpc routingKey to publish
   * @param props for request message, default null
   * @return a SyncVar that wrapps response received
   * @throws ShutdownSignalException if the connection dies during our wait
   * @throws IOException if an error is encountered
   */
  @throws(classOf[IOException])
  @throws(classOf[ShutdownSignalException])
  def asyncRpc(req: Any, routingKey: String, props: AMQP.BasicProperties = new AMQP.BasicProperties.Builder().build): SyncVar[Any] = {
    val syncVar = new SyncVar[Any]
    val replyId = continuationMap synchronized {
      correlationId += 1
      val replyIdx = correlationId.toString
      continuationMap.put(replyIdx, syncVar)
      replyIdx
    }

    try {
      checkConsumer
    } catch {
      case ex: Throwable =>
        log.warning(ex.getMessage)
        syncVar.set(Evt.Error(ex.getMessage))
        return syncVar
    }

    val reqProps = props.builder.correlationId(replyId).replyTo(replyQueue).build

    publish(req, exchange, routingKey, reqProps)

    syncVar
  }

  override protected def useActor = false
  override def process(res: Any, props: AMQP.BasicProperties = new AMQP.BasicProperties.Builder().build, envelope: Envelope = null) {
    val replyId = props.getCorrelationId
//    log.info("Reply id=" + replyId)
    continuationMap synchronized {
      continuationMap.remove(replyId) match{
        case Some(syncVar) => syncVar.set(res)
        case None =>
      }
    }
  }

//  class SyncVarSetterProcessor extends Processor{
//    def process(amqpMsg: AMQPMessage) {
//      amqpMsg match {
//        case AMQPMessage(res, props, _) =>
//          val replyId = props.getCorrelationId
////          log.info("Reply id=" + replyId)
//          continuationMap synchronized {
//            continuationMap.remove(replyId) match{
//              case Some(syncVar) => syncVar.set(res)
//              case None =>
//            }
//          }
//
//        case x => log.warning("Wrong msg: " + x)
//      }
//    }
//  }
}