package org.aiotrade.lib.math.random

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.URL
import java.text.MessageFormat
import java.util.concurrent.locks.ReentrantLock

/**
 * Connects to the <a href="http://www.random.org" target="_top">random.org</a>
 * website (via HTTPS) and downloads a set of random bits to use as seed data.  It
 * is generally better to use the {@link DevRandomSeedGenerator} where possible,
 * as it should be much quicker. This seed generator is most useful on Microsoft
 * Windows and other platforms that do not provide {@literal /dev/random}.
 * @author Daniel Dyer
 */
object RandomDotOrgSeedGenerator extends SeedGenerator {
  private val BASE_URL = "https://www.random.org"

  /** The URL from which the random bytes are retrieved. */
  private val RANDOM_URL = BASE_URL + "/integers/?num={0,number,0}&min=0&max=255&col=1&base=16&format=plain&rnd=new"

  /** Used to identify the client to the random.org service. */
  private val USER_AGENT = getClass.getName

  /** Random.org does not allow requests for more than 10k integers at once. */
  private val MAX_REQUEST_SIZE = 10000
  
  private val cacheLock = new ReentrantLock()
  private var cache = new Array[Byte](1024)
  private var cacheOffset = cache.length

  /**
   * {@inheritDoc}
   */    
  @throws(classOf[SeedException])
  def generateSeed(length: Int): Array[Byte] = {
    val seedData = new Array[Byte](length)
    try {
      cacheLock.lock
      var count = 0
      while (count < length) {
        if (cacheOffset < cache.length) {
          val numberOfBytes = math.min(length - count, cache.length - cacheOffset)
          System.arraycopy(cache, cacheOffset, seedData, count, numberOfBytes)
          count += numberOfBytes
          cacheOffset += numberOfBytes
        } else {
          refreshCache(length - count)
        }
      }
    } catch {
      case ex: IOException =>
        throw SeedException("Failed downloading bytes from " + BASE_URL, ex)
      case ex: SecurityException =>
        // Might be thrown if resource access is restricted (such as in an applet sandbox).
        throw SeedException("SecurityManager prevented access to " + BASE_URL, ex)
    } finally {
      cacheLock.unlock
    }
    seedData
  }


  /**
   * @param requiredBytes The preferred number of bytes to request from random.org.
   * The implementation may request more and cache the excess (to avoid making lots
   * of small requests).  Alternatively, it may request fewer if the required number
   * is greater than that permitted by random.org for a single request.
   * @throws IOException If there is a problem downloading the random bits.
   */
  @throws(classOf[IOException])
  private def refreshCache(requiredBytes: Int) {
    var numberOfBytes = math.max(requiredBytes, cache.length)
    numberOfBytes = math.min(numberOfBytes, MAX_REQUEST_SIZE)
    if (numberOfBytes != cache.length) {
      cache = new Array[Byte](numberOfBytes)
      cacheOffset = numberOfBytes
    }
    val url = new URL(MessageFormat.format(RANDOM_URL, numberOfBytes.asInstanceOf[AnyRef]))
    val connection = url.openConnection
    connection.setRequestProperty("User-Agent", USER_AGENT)
    val reader = new BufferedReader(new InputStreamReader(connection.getInputStream))
        
    try {
      var index = -1
      var line = reader.readLine
      while (line != null) {
        index += 1
        cache(index) = java.lang.Integer.parseInt(line, 16).toByte
        line = reader.readLine
      }
      if (index < cache.length - 1) {
        throw new IOException("Insufficient data received.")
      }
      cacheOffset = 0
    } finally {
      reader.close
    }
  }

  override
  def toString = {
    BASE_URL
  }
}

