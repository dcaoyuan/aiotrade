package org.aiotrade.lib.util

/**
 *
 * @author Caoyuan Deng
 */
object WindowManager {
  private lazy val manager: WindowManager = ServiceLoader.load(classOf[WindowManager]).iterator.next

  def apply(): WindowManager = manager
}

trait WindowManager {
  
  def statusText: String
  def statusText_=(text: String)

  def toolbarConfiguration: String
  def toolbarConfiguration_=(name: String)
  
}
