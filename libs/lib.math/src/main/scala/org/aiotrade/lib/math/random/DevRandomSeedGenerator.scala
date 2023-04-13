package org.aiotrade.lib.math.random

import java.io.File
import java.io.FileInputStream
import java.io.IOException

/**
 * RNG seed strategy that gets data from {@literal /dev/urandom} on systems that provide it (e.g.
 * Solaris/Linux).  If {@literal /dev/random} does not exist or is not accessible, a {@link
 * SeedException} is thrown.  The point of pulling from /dev/urandom instead of from /dev/random
 * is that /dev/random will block if it doesn't think it has enough entropy.  In most production
 * applications of Mahout, that really isn't necessary.
 */
object DevURandomSeedGenerator extends FileRandomSeedGenerator("/dev/urandom")

/**
 * RNG seed strategy that gets data from {@literal /dev/random} on systems
 * that provide it (e.g. Solaris/Linux).  If {@literal /dev/random} does not
 * exist or is not accessible, a {@link SeedException} is thrown.
 */
object DevRandomSeedGenerator extends FileRandomSeedGenerator("/dev/random")

abstract class FileRandomSeedGenerator(filePath: String) extends SeedGenerator {
  private val DEV_RANDOM = new File(filePath)
  
  /**
   * {@inheritDoc}
   * @return The requested number of random bytes, read directly from
   * {@literal /dev/random}.
   * @throws SeedException If {@literal /dev/random} does not exist or is
   * not accessible
   */
  @throws(classOf[SeedException])
  def generateSeed(length: Int): Array[Byte] =  {
    var in: FileInputStream = null
    try {
      in = new FileInputStream(DEV_RANDOM)
      val bytes = new Array[Byte](length)
      var count = 0
      while (count < length) {
        val bytesRead = in.read(bytes, count, length - count)
        if (bytesRead == -1) {
          throw SeedException("EOF encountered reading random data.")
        }
        count += bytesRead
      }
      bytes
    } catch {
      case ex: IOException =>
        throw SeedException("Failed reading from " + DEV_RANDOM.getName, ex)
      case ex: SecurityException =>
        // Might be thrown if resource access is restricted (such as in
        // an applet sandbox).
        throw SeedException("SecurityManager prevented access to " + DEV_RANDOM.getName, ex)
    } finally {
      if (in != null) {
        try {
          in.close
        } catch {
          case ex: IOException =>
        }
      }
    }
  }


  override
  def toString = {
    filePath
  }
}
