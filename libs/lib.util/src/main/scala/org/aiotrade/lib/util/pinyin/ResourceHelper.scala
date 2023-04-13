package org.aiotrade.lib.util.pinyin

import java.io.BufferedInputStream

/**
 * Helper class for file resources
 * 
 */
object ResourceHelper {
  private val classLoader = Thread.currentThread.getContextClassLoader
  /**
   * @param resourceName
   * @return resource (mainly file in file system or file in compressed
   *         package) as BufferedInputStream
   */
  def getResourceInputStream(resourceName: String): BufferedInputStream =
    new BufferedInputStream(classLoader.getResourceAsStream(resourceName))
}
