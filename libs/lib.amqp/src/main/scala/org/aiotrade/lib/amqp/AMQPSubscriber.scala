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
import java.util.logging.Level
import java.util.logging.Logger

/**
 *
 * @author Caoyuan Deng
 */
class AMQPSubscriber(factory: ConnectionFactory, exchange: String, isAutoAck: Boolean = true, durable: Boolean = false) extends AMQPDispatcher(factory, exchange) {
  private val log = Logger.getLogger(this.getClass.getName)

  final class Queue private (val name: String, val durable: Boolean, val exclusive: Boolean, val autoDelete: Boolean) {
    override def equals(that: Any) = that match {
      case x: Queue => x.name == name
      case _ => false
    }

    override def hashCode = name.hashCode
    override def toString = "Queue(" + name + ", durable=" + durable + ", exclusive=" + exclusive + ", autoDelete=" + autoDelete + ")"
  }

  object Queue {
    def apply(name: String) = new Queue(name, false, false, true)
    def apply(name: String, durable: Boolean, exclusive: Boolean, autoDelete: Boolean) =
      new Queue(name, durable, exclusive, autoDelete)

    def unapply(queue: Queue) = Some((queue.name, queue.durable, queue.exclusive, queue.autoDelete))
  }

  private case class Topic(name: String, bindingQueue: String) {
    override def toString = "Topic(" + name + " ~> " + bindingQueue + ")"
  }

  private case class ConsumeQueue(queue: Queue, isDefult: Boolean)
  private case class SubscribeTopic(topic: Topic)
  private case class UnsubscribeTopic(topic: Topic)

  private var _defaultQueue: Option[Queue] = None
  private var _consumingQueues = Map[String, Queue]()
  private var _subscribedTopics = Set[Topic]()

  /**
   * @Note Since we cannot guarantee when will AMQPConnected received (before or after consumeQueue/subscribeTopic calls),
   * we should be aware the concurrent issue. So use actor + messages here to avoid concurrent issue
   */
  reactions += {
    case AMQPConnected => 
      // When reconnecting, should resubscribing existed queues and topics:
      log.info("Got AMQPConnected event, try to (re)subscribing : " + _consumingQueues + ", " + _subscribedTopics)
      val default = _defaultQueue
      _consumingQueues foreach {case (qname, queue) => doConsumeQueue(queue)}
      _defaultQueue = default
      _subscribedTopics foreach doSubscribeTopic
    case ConsumeQueue(queue, isDefault) =>
      doConsumeQueue(queue, isDefault)
    case SubscribeTopic(topic) =>
      doSubscribeTopic(topic)
    case UnsubscribeTopic(topic) =>
      doUnsubscribeTopic(topic)
  }

  override protected def configure(channel: Channel): Option[Consumer] = {
    Some(new AMQPConsumer(channel, isAutoAck))
  }

  /**
   * Consumer queue. If this queue does not exist yet, also delcare it here.
   */
  def consumeQueue(name: String, durable: Boolean = false, exclusive: Boolean = false, autoDelete: Boolean = true, isDefault: Boolean = false) {
    publish(ConsumeQueue(Queue(name, durable, exclusive, autoDelete), isDefault))
  }

  def subscribeTopic(name: String, bindingQueue: String = null) {
    publish(SubscribeTopic(Topic(name, bindingQueue)))
  }

  def unsubscribeTopic(name: String, bindingQueue: String = null) {
    publish(UnsubscribeTopic(Topic(name, bindingQueue)))
  }

  private def doConsumeQueue(queue: Queue, isDefault: Boolean = false) {
    if (!isConnected) {
      log.severe("Should connect before consume " + queue)
    } else {
      if (isDefault) {
        _defaultQueue = Some(queue)
      } else {
        if (_defaultQueue.isEmpty) {
          _defaultQueue = Some(queue)
        }
      }
      _consumingQueues += (queue.name -> queue)
      
      for (ch <- channel; cs <- consumer) {
        log.log(Level.INFO, "AMQPSubscriber exchange declaring [" + exchange + ", direct, " +  durable + "]")
        ch.exchangeDeclare(exchange, "direct", durable)

        // @Todo We need a non-exclusive queue, so when reconnected, this queue can be used by the new created connection.
        // string queue, boolean durable, boolean exclusive, boolean autoDelete, Map<String, Object> arguments
        try {
          ch.queueDeclare(queue.name, queue.durable, queue.exclusive, queue.autoDelete, null)
        } catch {
          case ex: Throwable => log.log(Level.SEVERE, ex.getMessage, ex)
        }
      
        ch.basicConsume(queue.name, cs.asInstanceOf[AMQPConsumer].isAutoAck, cs)
        log.info("Declared and Consuming queue: " + queue.name + ". The defaultQueue=" + _defaultQueue.map(_.name))
      }
    }
  }

  private def doSubscribeTopic(topic: Topic) {
    if (!isConnected) {
      log.severe("Should connect before subscribe " + topic)
    } else {
      if (_consumingQueues.isEmpty) {
        log.severe("At least one queue should be declared before subscribe " + topic)
      } else {
        val queueOpt = topic.bindingQueue match {
          case null => _defaultQueue
          case x => _consumingQueues.get(x)
        }

        queueOpt match {
          case None => log.severe("Queue does not in consuming when subscribe " + topic + " on it.")
          case Some(q) =>
            _subscribedTopics += Topic(topic.name, q.name)
            for (ch <- channel) {
              ch.queueBind(q.name, exchange, topic.name)
              log.info("Subscribed topic: " + topic)
            }
        }
      }
    }

  }

  private def doUnsubscribeTopic(topic: Topic) {
    _subscribedTopics -= topic // remove it whatever

    if (!isConnected) {
      log.warning("Should connect before unsubscribe " + topic)
    } else {
      val queueOpt = topic.bindingQueue match {
        case null => _defaultQueue
        case x => _consumingQueues.get(x)
      }

      queueOpt match {
        case None => log.warning("Queue does not in consuming when ubsubscribe " + topic + " on it.")
        case Some(q) =>
          for (ch <- channel) {
            ch.queueUnbind(q.name, exchange, topic.name)
          }
      }
    }
  }

}



