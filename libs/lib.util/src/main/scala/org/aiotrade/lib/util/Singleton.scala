package org.aiotrade.lib.util

/**
 * Used to generate a static method for getSingleton, then
 * we can register it in NetBeans' layer.xml as:
 *     <file name="YahooTicker.instance">
 *         <attr name="instanceCreate" methodvalue="org.aiotrade.lib.dataserver.yahoo.YahooTickerServer.getSingleton"/>
 *     </file>
 *
 * @see http://bits.netbeans.org/dev/javadoc/org-openide-util/org/openide/util/doc-files/api.html#instances
 */
trait Singleton {
  def getSingleton: this.type
}