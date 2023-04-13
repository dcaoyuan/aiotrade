/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved.
 * 
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * 
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 * 
 * Contributor(s):
 * 
 * Portions Copyrighted 2008 Sun Microsystems, Inc.
 */

package org.aiotrade.modules.quicksearch.web;

import java.io.UnsupportedEncodingException;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import scala.collection.mutable.ArrayBuffer


/**
 * Parse raw HTML from Google search
 * 
 * @author S. Aubrecht
 */
class Result {

  private var items = new ArrayBuffer[Item](Query.MAX_NUM_OF_RESULTS)
  var isSearchFinished = false
  private val pattern = Pattern.compile("<a\\s+href\\s*=\\s*\"(.*?)\"[^>]*>(.*?)</a>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE)

  def getItems: List[Item] = {
    items.toList
  }
        
  def parse($html: String, currentSearchOffset: Int) {
    isSearchFinished = true
    items.clear
    var html = $html
    try {
      html = new String(html.getBytes, "UTF-8") //NOI18N
    } catch {case ex: UnsupportedEncodingException => Logger.getLogger(classOf[Result].getName).log(Level.FINE, null, ex)}

    val m = pattern.matcher(html)
    while( m.find) {
      val url = m.group(1)
      var title = m.group(2)
      if (url.startsWith("/")) {//NOI18N
        //look for previous/next links
        val searchOffset = findSearchOffset( url )
        if (searchOffset > currentSearchOffset)
          isSearchFinished = false
      } else {
        if (!url.contains("google.com") ) {//NOI18N
          title = "<html>" + title //NOI18N
          val si = Item(url, title, null)
          items += si
        }
      }
    }
  }
    
  def filterUrl(urlPatterns: Array[String]) {
    if( null == urlPatterns || urlPatterns.length == 0) {
      return
    }
    
    val filteredItems = new ArrayBuffer[Item](items.size)
    for (item <- items) {
      if (urlPatterns exists (x => x.length != 0 && item.url.toLowerCase(Locale.ENGLISH).matches(x))) {
        filteredItems += item
      }
    }
    items = filteredItems
  }
    
  private def findSearchOffset(url: String): Int = {
    val startIndex = url.indexOf( "&amp;start=" )  //NOI18N
    if( startIndex < 0 )
      return -1
        
    var endIndex = url.indexOf("&amp;", startIndex + 1)  //NOI18N
    if (endIndex < 0 )
      endIndex = url.length
    if (endIndex < startIndex)
      return -1
    
    val offset = url.substring(startIndex, endIndex)
    try {
      return Integer.parseInt(offset)
    } catch {case ex: NumberFormatException =>}

    return -1
  }
}
