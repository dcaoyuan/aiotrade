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
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.Consumer
import com.rabbitmq.client.DefaultConsumer
import com.rabbitmq.client.Envelope
import com.rabbitmq.client.ShutdownListener
import com.rabbitmq.client.ShutdownSignalException
import java.io.IOException
import java.io.InvalidClassException
import java.util.Timer
import java.util.TimerTask
import java.util.logging.Logger
import java.util.logging.Level

/**
 * @Note If we use plain sync Publisher/Reactor instead of actor based async model, it will because:
 * 1. It seems that when actor model is mixed with a hard coded thread (AMQPConnection has a MainLoop thread),
 *    the scheduler of actor may deley delivery message, that causes unacceptable latency for amqp messages
 * 2. Unlick indicator, tser etc, we do not need async, parallel scale for amcp clients
 */
import org.aiotrade.lib.avro.Msg
import org.aiotrade.lib.util.actors.Publisher
import org.aiotrade.lib.util.actors.Reactor

/*_ rabbitmqctl common usages:
 sudo rabbitmq-server -n rabbit@localhost &
 sudo rabbitmqctl -n rabbit@localhost stop

 sudo rabbitmqctl -n rabbit@localhost stop_app
 sudo rabbitmqctl -n rabbit@localhost reset
 sudo rabbitmqctl -n rabbit@localhost start_app
 sudo rabbitmqctl -n rabbit@localhost list_queues name messages messages_uncommitted messages_unacknowledged

 If encountered troubes when start the server up, since the tables in the mnesia
 database backing rabbitmq are locked (Don't know why this is the case). you can
 get this running again brute force styleee by deleting the database:

 sudo rm -rf /opt/local/var/lib/rabbitmq/mnesia/
 */

/*_
 * Option 1:
 * create one queue per consumer with several bindings, one for each stock.
 * Prices in this case will be sent with a topic routing key.
 *
 * Option 2:
 * Another option is to create one queue per stock. each consumer will be
 * subscribed to several queues. Messages will be sent with a direct routing key.
 *
 * Best Practice:
 * Option 1: should work fine, except there is no need to use a topic exchange.
 * Just use a direct exchange, one queue per user and for each of the stock
 * symbols a user is interested in create a binding between the user's queue
 * and the direct exchange.
 *
 * Option 2: each quote would only go to one consumer, which is probably not
 * what you want. In an AMQP system, to get the same message delivered to N
 * consumers you need (at least) N queues. Exchanges *copy* messages to queues,
 * whereas queues *round-robin* message delivery to consumers.
 */

case object AMQPConnected
case object AMQPDisconnected

object AMQPExchange {

  /**
   * Each AMQP broker declares one instance of each supported exchange type on it's
   * own (for every virtual host). These exchanges are named after the their type
   * with a prefix of amq., e.g. amq.fanout. The empty exchange name is an alias
   * for amq.direct. For this default direct exchange (and only for that) the broker
   * also declares a binding for every queue in the system with the binding key
   * being identical to the queue name.
   *
   * This behaviour implies that any queue on the system can be written into by
   * publishing a message to the default direct exchange with it's routing-key
   * property being equal to the name of the queue.
   */
  val defaultDirect = "" // amp.direct

  sealed trait AMQPExchange
  case object Direct extends AMQPExchange {override def toString = "direct"}
  case object Topic  extends AMQPExchange {override def toString = "topic" }
  case object Fanout extends AMQPExchange {override def toString = "fanout"}
  case object Match  extends AMQPExchange {override def toString = "match" }
}

object AMQPDispatcher {
  private val defaultReconnectDelay = 5000
  private val maxReconnectDelay = 1800000 // = 30 * 60 * 1000  half an hour.
  private lazy val timer = new Timer("AMQPReconnectTimer")
}

/**
 * The dispatcher that listens over the AMQP message endpoint.
 * It manages a list of subscribers to the trade message and also sends AMQP
 * messages coming in to the queue/exchange to the list of observers.
 *
 * @author Caoyuan Deng
 */
abstract class AMQPDispatcher(factory: ConnectionFactory, val exchange: String) extends Publisher {
  private val log = Logger.getLogger(getClass.getName)

//  protected def useActor = true

  private case class State(connection: Option[Connection], channel: Option[Channel], consumer: Option[Consumer])
  private var state = State(None, None, None)

  private var reconnectDelay: Long = AMQPDispatcher.defaultReconnectDelay

  /**
   * Connect only when start, so we can control it to connect at a appropriate time,
   * for instance, all processors are ready. Otherwise, the messages may have been
   * consumered before processors ready.
   */
  def connect: this.type = {
    doConnect
    this
  }

  final def connection = state.connection
  final def consumer = state.consumer
  final def channel = state.channel

  private val shutdownListener = new ShutdownListener {
    def shutdownCompleted(cause: ShutdownSignalException) {
      publish(AMQPDisconnected)
      reconnect(cause)
    }
  }

  @throws(classOf[IOException])
  private def doConnect {
    log.info("Begin to connect " + factory.getHost + ":" + factory.getPort + "...")

    val connectTry = try {
      val conn = factory.newConnection
      // @Note: Should listen to connection instead of channel on ShutdownSignalException,
      // @see com.rabbitmq.client.impl.AMQPConnection.MainLoop
      conn.addShutdownListener(shutdownListener)

      Left(conn)
    } catch {
      case ex: Throwable => Right(ex)
    }

    connectTry match {
      case Left(conn) =>
        // we won't catch exceptions thrown during the following procedure, since we need them to fire ShutdownSignalException
        try{
          val channel = conn.createChannel
          val consumer = configure(channel)

          state = State(Option(conn), Option(channel), consumer)

          log.info("Successfully connected at: " + conn.getAddress.getHostAddress + ":" + conn.getPort)
          reconnectDelay = AMQPDispatcher.defaultReconnectDelay
          publish(AMQPConnected)
        }
        catch{
          case ex: Throwable => // don't log ex here, we hope ShutdownListener will give us the cause
            log.log(Level.SEVERE, ex.getMessage, ex)
            publish(AMQPDisconnected)
            // @Note **only** when there is none created connection, we'll try to reconnect here,
            // let shutdown listener to handle all other reconnetion needs
            reconnect(ex)
        }
      case Right(ex) =>
        publish(AMQPDisconnected)
        // @Note **only** when there is none created connection, we'll try to reconnect here,
        // let shutdown listener to handle all other reconnetion needs
        reconnect(ex)
    }
  }

  private def reconnect(cause: Throwable) {
    log.warning("Will try to reconnect in " + reconnectDelay + " ms, the cause is:")
    log.log(Level.WARNING, cause.getMessage, cause)

    disconnect

    AMQPDispatcher.timer.schedule(new TimerTask {
        def run {
          reconnectDelay *= 2
          if (reconnectDelay > AMQPDispatcher.maxReconnectDelay) reconnectDelay = AMQPDispatcher.maxReconnectDelay
          doConnect
        }
      }, reconnectDelay)
  }

  private def disconnect {
    channel foreach {_ch =>
      try {
        consumer foreach {case x: DefaultConsumer => _ch.basicCancel(x.getConsumerTag)}
        _ch.close
      } catch {
        case ex: Throwable => log.log(Level.SEVERE, ex.getMessage, ex)
      }
    }

    connection foreach {_conn =>
      _conn.removeShutdownListener(shutdownListener)
      if (_conn.isOpen) {
        try {
          _conn.close
          log.log(Level.FINEST, "Disconnected AMQP connection at %s:%s [%s]", Array(factory.getHost, factory.getPort, this))
        } catch {
          case _: Throwable =>
        }
      }
    }
  }

  def isConnected = connection.isDefined && connection.get.isOpen

  /**
   * Registers queue and consumer.
   * @param mandatory means:
   *    Put this message on at least one queue. If you can't, send it back to me.
   * @param immediate means:
   *    If there is at least one consumer connected to my queue that can take delivery of a message
   *    right this moment, deliver this message to them immediately. If there are no consumers
   *    connected then there's no point in having my message consumed later and they'll never see it.
   *    They snooze, they lose
   *
   * @throws IOException if an error is encountered
   * @return the newly created and registered (queue, consumer)
   */
  @throws(classOf[IOException])
  protected def configure(channel: Channel): Option[Consumer]

  @throws(classOf[IOException])
  def publish(content: Any, exchange: String, routingKey: String, props: AMQP.BasicProperties = new AMQP.BasicProperties.Builder().build,
              mandatory: Boolean = false, immediate: Boolean = false
  ) {
    channel foreach {_ch =>
      val contentType = props.getContentType match {
        case null | "" => DEFAULT_CONTENT_TYPE
        case x => ContentType(x)
      }

      val headers = Option(props.getHeaders) getOrElse new java.util.HashMap[String, AnyRef](1)

      try {
        import ContentType._
        val encodedBody = contentType.mimeType match {
          case JSON.mimeType =>
            content match {
              case msg: Msg[_] => headers.put(TAG, msg.tag.asInstanceOf[AnyRef])
              case _ => // todo
            }
            Serializer.encodeJson(content)
          case AVRO.mimeType =>
            content match {
              case msg: Msg[_] => headers.put(TAG, msg.tag.asInstanceOf[AnyRef])
              case _ => // todo
            }
            Serializer.encodeAvro(content)

          case JAVA_SERIALIZED_OBJECT.mimeType => Serializer.encodeJava(content)
          case OCTET_STREAM.mimeType => content.asInstanceOf[Array[Byte]]
          case _ => content.asInstanceOf[Array[Byte]]
        }

        val contentEncoding = props.getContentEncoding match {
          case null | "" => GZIP
          case x => x
        }

        val body = contentEncoding match {
          case GZIP => Serializer.gzip(encodedBody)
          case LZMA => Serializer.lzma(encodedBody)
          case _ => encodedBody
        }

        val outProps = props.builder.contentType(contentType.mimeType).contentEncoding(contentEncoding).headers(headers).build

        _ch.basicPublish(exchange, routingKey, mandatory, immediate, outProps, body)
        log.fine(content + " sent: routingKey=" + routingKey + " size=" + body.length)
      } catch {
        case ex: Throwable => log.log(Level.WARNING, ex.getMessage, ex)
      }
    }
  }

  protected def deleteQueue(queue: String) {
    channel foreach {_ch =>
      try {
        // Check if the queue existed, if existed, will return a declareOk object, otherwise will throw IOException
        val declareOk = _ch.queueDeclarePassive(queue)
        try {
          // the exception thrown here will destroy the connection too, so use it carefully
          _ch.queueDelete(queue)
          log.info("Deleted queue: " + queue)
        } catch {
          case ex: Throwable => log.log(Level.SEVERE, ex.getMessage, ex)
        }
      } catch {
        case ex: IOException => // queue doesn't exist
        case ex: Throwable => log.log(Level.WARNING, ex.getMessage, ex)
      }
    }
  }

  class AMQPConsumer(channel: Channel, val isAutoAck: Boolean) extends DefaultConsumer(channel) {
    private val log = Logger.getLogger(this.getClass.getName)

    // When this is non-null the queue is in shutdown mode and nextDelivery should
    // throw a shutdown signal exception.
    @volatile private var _shutdown: ShutdownSignalException = _

    override def handleShutdownSignal(consumerTag: String, sig: ShutdownSignalException ) {
      _shutdown = sig
    }

    def handleAck(_isAutoAck: Boolean, _channel: Channel, _envelope: Envelope) {
      if (!_isAutoAck) {
        try {
          // Params:
          //   deliveryTag - the tag from the received AMQP.Basic.GetOk or AMQP.Basic.Deliver
          //   multiple - true  to acknowledge all messages up to and including the supplied delivery tag;
          //              false to acknowledge just the supplied delivery tag.
          _channel.basicAck(_envelope.getDeliveryTag, false)
        } catch {
          case ex: Throwable => log.log(Level.WARNING, ex.getMessage, ex)
        }
      }
    }

    @throws(classOf[IOException])
    override def handleDelivery(tag: String, envelope: Envelope, props: AMQP.BasicProperties, body: Array[Byte]) {
      // If needs ack, do it right now to avoid the queue on server is blocked.
      // @Note: when autoAck is set false, messages will be blocked until an ack to broker,
      // so should ack it. (Although prefetch may deliver more than one message to consumer)
      handleAck(isAutoAck, channel, envelope)

      log.fine("Got amqp message: " + (body.length / 1024.0) + "k" )

      val unzippedBody = props.getContentEncoding match {
        case GZIP => Serializer.ungzip(body)
        case LZMA => Serializer.unlzma(body)
        case _ => body
      }

      val contentType = props.getContentType match {
        case null | "" =>  DEFAULT_CONTENT_TYPE
        case x => ContentType(x)
      }

      val headers = Option(props.getHeaders) getOrElse java.util.Collections.emptyMap[String, AnyRef]

      try {
        import ContentType._
        val content = contentType.mimeType match {
          case JSON.mimeType => headers.get(TAG) match {
              case tag: java.lang.Integer =>
                val value = Serializer.decodeJson(unzippedBody, tag.intValue)
                Msg(tag.intValue, value)
              case _ => null
            }
          case AVRO.mimeType => headers.get(TAG) match {
              case tag: java.lang.Integer =>
                val value = Serializer.decodeAvro(unzippedBody, tag.intValue)
                Msg(tag.intValue, value)
              case _ => null
            }

          case JAVA_SERIALIZED_OBJECT.mimeType => Serializer.decodeJava(unzippedBody)
          case OCTET_STREAM.mimeType => unzippedBody
          case _ => unzippedBody
        }

        val fwProps = props.builder.contentType(contentType.mimeType).headers(headers).build

        // forward to interested observers for further relay
        if (useActor){
//          for(l <- listeners if l != this) log.info(l + ",  state=" + l.getState)
          publish(AMQPMessage(content, fwProps, envelope))
        }
        else process(content, fwProps, envelope)

        log.fine("Forward amqp message: " + content)
      } catch {
        // should catch it when old version classes were sent by old version of clients.
        case ex: InvalidClassException => log.log(Level.WARNING, ex.getMessage, ex)
        case ex: Throwable => log.log(Level.WARNING, ex.getMessage, ex)
      }
    }
  }

  protected def useActor = true
  protected def process(res: Any, props: AMQP.BasicProperties = new AMQP.BasicProperties.Builder().build, envelope: Envelope = null){}

  /**
   * Hold strong refs of processors to avoid them to be GCed
   */
  private[amqp] var processors: List[Processor] = Nil

  /**
   * Processor that will automatically added as listener of this AMQPDispatcher
   * and process AMQPMessage via process(msg)
   */
  abstract class Processor extends Reactor {
    processors synchronized {processors ::= this}

    reactions += {
      case amqpMsg: AMQPMessage => process(amqpMsg)
    }
    listenTo(AMQPDispatcher.this)

    protected def process(msg: AMQPMessage)
  }

}
