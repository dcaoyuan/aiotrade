// Copyright 2007-2009 Christian d'Heureuse, Inventec Informatik AG, Zurich, Switzerland
// www.source-code.biz, www.inventec.ch/chdh
//
// This module is multi-licensed and may be used under the terms
// of any of the following licenses:
//
//  EPL, Eclipse Public License, http://www.eclipse.org/legal
//  LGPL, GNU Lesser General Public License, http://www.gnu.org/licenses/lgpl.html
//  MPL, Mozilla Public License 1.1, http://www.mozilla.org/MPL
//
// Please contact the author if you need another license.
// This module is provided "as is", without warranties of any kind.

package org.aiotrade.modules.ui

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.LinkedList;
import javax.sql.ConnectionEvent;
import javax.sql.ConnectionEventListener;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.PooledConnection;

/**
 * A simple standalone JDBC connection pool manager.
 * <p>
 * The public methods of this class are thread-safe.
 * <p>
 * Home page: <a href="http://www.source-code.biz">www.source-code.biz</a><br>
 * Author: Christian d'Heureuse, Inventec Informatik AG, Zurich, Switzerland<br>
 * Multi-licensed: EPL/LGPL/MPL.
 * <p>
 * 2007-06-21: Constructor with a timeout parameter added.<br>
 * 2008-05-03: Additional licenses added (EPL/MPL).<br>
 * 2009-06-26: Variable recycledConnections changed from Stack to Queue, so that
 *   the unused connections are reused in a circular manner.
 *   Thanks to Daniel Jurado for the tip.<br>
 * 2009-08-21: ArrayDeque (which was introduced with change 2009-06-26) replaced
 *   by LinkedList, because ArrayDeque is only available since Java 1.6 and we want
 *   to keep MiniConnectionPoolManager compatible with Java 1.5.<br>
 *
 * @param dataSource      the data source for the connections.
 * @param maxConnections  the maximum number of connections.
 * @param timeout         the maximum time in seconds to wait for a free connection.
 */
class MiniConnectionPoolManager(dataSource: ConnectionPoolDataSource, maxConnections: Int, timeout: Int) {
  if (maxConnections < 1) throw new IllegalArgumentException("Invalid maxConnections value.")

  private val logWriter = try {
    dataSource.getLogWriter
  } catch {case e: SQLException => null}
  private val semaphore = new Semaphore(maxConnections, true)
  private val recycledConnections = new LinkedList[PooledConnection]
  private val poolConnectionEventListener = new PoolConnectionEventListener
  private var activeConnections = 0
  private var isDisposed = false

  /**
   * Thrown in {@link #getConnection()} when no free connection becomes available within <code>timeout</code> seconds.
   */
  @SerialVersionUID(1)
  object TimeoutException extends RuntimeException("Timeout while waiting for a free database connection.")

  /**
   * Constructs a MiniConnectionPoolManager object with a timeout of 60 seconds.
   * @param dataSource      the data source for the connections.
   * @param maxConnections  the maximum number of connections.
   */
  def this(dataSource: ConnectionPoolDataSource, maxConnections: Int) = this (dataSource, maxConnections, 60)

  /**
   * Closes all unused pooled connections.
   */
  @throws(classOf[SQLException])
  def dispose: Unit = synchronized {
    if (isDisposed) return

    isDisposed = true
    var e: SQLException = null
    while (!recycledConnections.isEmpty) {
      val pconn = recycledConnections.remove
      try {
        pconn.close
      } catch {case e2: SQLException => if (e == null) e = e2}
    }
    if (e != null) throw e
  }

  /**
   * Retrieves a connection from the connection pool.
   * If <code>maxConnections</code> connections are already in use, the method
   * waits until a connection becomes available or <code>timeout</code> seconds elapsed.
   * When the application is finished using the connection, it must close it
   * in order to return it to the pool.
   * @return a new Connection object.
   * @throws TimeoutException when no connection becomes available within <code>timeout</code> seconds.
   */
  @throws(classOf[SQLException])
  def getConnection: Connection = {
    // This routine is unsynchronized, because semaphore.tryAcquire() may block.
    synchronized {
      if (isDisposed) throw new IllegalStateException("Connection pool has been disposed.")
    }
    try {
      if (!semaphore.tryAcquire(timeout, TimeUnit.SECONDS)) throw TimeoutException
    } catch {case e: InterruptedException => throw new RuntimeException("Interrupted while waiting for a database connection.", e)}
    var ok = false
    try {
      val conn = getConnection2
      ok = true
      return conn
    } finally {
      if (!ok) semaphore.release
    }
  }

  @throws(classOf[SQLException])
  private def getConnection2: Connection = synchronized {
    if (isDisposed) throw new IllegalStateException("Connection pool has been disposed.")   // test again with lock
    var pconn = if (!recycledConnections.isEmpty) {
      recycledConnections.remove
    } else {
      dataSource.getPooledConnection
    }
    val conn = pconn.getConnection
    activeConnections += 1
    pconn.addConnectionEventListener(poolConnectionEventListener)
    assertInnerState
    
    conn
  }

  private def recycleConnection(pconn: PooledConnection): Unit = synchronized {
    if (isDisposed) {
      disposeConnection(pconn)
      return
    }
    if (activeConnections <= 0) throw new AssertionError
    activeConnections -= 1
    semaphore.release
    recycledConnections.add(pconn)
    assertInnerState
  }

  private def disposeConnection(pconn: PooledConnection): Unit = synchronized {
    if (activeConnections <= 0) throw new AssertionError
    activeConnections -= 1
    semaphore.release
    closeConnectionNoEx(pconn)
    assertInnerState
  }

  private def closeConnectionNoEx(pconn: PooledConnection) {
    try {
      pconn.close
    } catch {case e: SQLException => log("Error while closing database connection: " + e.toString)}
  }

  private def log(msg: String) {
    val s = "MiniConnectionPoolManager: " + msg
    try {
      if (logWriter == null)
        System.err.println(s)
      else
        logWriter.println(s)
    } catch {case e: Exception =>}
  }

  private def assertInnerState {
    if (activeConnections < 0) throw new AssertionError
    if (activeConnections + recycledConnections.size   > maxConnections) throw new AssertionError
    if (activeConnections + semaphore.availablePermits > maxConnections) throw new AssertionError
  }

  private class PoolConnectionEventListener extends ConnectionEventListener {
    def connectionClosed(event: ConnectionEvent) {
      val pconn = event.getSource.asInstanceOf[PooledConnection]
      pconn.removeConnectionEventListener(this)
      recycleConnection(pconn)
    }

    def connectionErrorOccurred(event: ConnectionEvent) {
      val pconn = event.getSource.asInstanceOf[PooledConnection]
      pconn.removeConnectionEventListener(this)
      disposeConnection(pconn)
    }
  }

  /**
   * Returns the number of active (open) connections of this pool.
   * This is the number of <code>Connection</code> objects that have been
   * issued by {@link #getConnection()} for which <code>Connection.close()</code>
   * has not yet been called.
   * @return the number of active connections.
   **/
  def getActiveConnections: Int = synchronized {
    activeConnections
  }

} // end class MiniConnectionPoolManager
