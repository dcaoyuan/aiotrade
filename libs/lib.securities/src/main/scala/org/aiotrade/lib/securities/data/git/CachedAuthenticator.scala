package org.aiotrade.lib.securities.data.git

import java.net.Authenticator
import java.net.PasswordAuthentication

import CachedAuthenticator._
abstract class CachedAuthenticator extends Authenticator {

  override protected def getPasswordAuthentication: PasswordAuthentication = {
    val host = getRequestingHost
    val port = getRequestingPort
    cached find {ca => ca.host.equals(host) && ca.port == port} match {
      case Some(x) => x.toPasswordAuthentication
      case None =>
        val pa = promptPasswordAuthentication
        if (pa != null) {
          val ca = CachedAuthentication(host, port, pa.getUserName, new String(pa.getPassword))
          add(ca)
          ca.toPasswordAuthentication
        } else null
    }
  }

  /**
   * Prompt for and request authentication from the end-user.
   *
   * @return the authentication data; null if the user canceled the request
   *         and does not want to continue.
   */
  protected def promptPasswordAuthentication: PasswordAuthentication
}

object CachedAuthenticator {
  private var cached = List[CachedAuthentication]()
  
  /**
   * Add a cached authentication for future use.
   *
   * @param ca
   *            the information we should remember.
   */
  def add(ca: CachedAuthentication) {
    cached ::= ca
  }
  
  /** 
   * Authentication data to remember and reuse. 
   */
  final case class CachedAuthentication(host: String, port: Int, user: String, pass: String) {
    def toPasswordAuthentication = new PasswordAuthentication(user, pass.toCharArray)
  }
}

