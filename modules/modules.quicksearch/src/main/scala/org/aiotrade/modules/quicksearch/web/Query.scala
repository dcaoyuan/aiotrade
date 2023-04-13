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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.util.NbBundle;

/**
 * Search Google for given text.
 * 
 * @author S. Aubrecht
 */

object Query {
  private lazy val instance = new Query
  def apply() = instance

  //restrict search for this site only
  //maximum number of search results requested
  val MAX_NUM_OF_RESULTS = 50
  private val SITE_SEARCH = getSiteSearch
  //google doesn't allow searching in site subfolders, so we must make sure
  //the links we found have the following patterns in their URLs
  private val URL_PATTERNS = getUrlPatterns


  private def getSiteSearch: String = {
    try {
      NbBundle.getMessage(classOf[Query], "quicksearch.web.site") //NOI18N
    } catch {case ex: MissingResourceException => null}
  }

  private def getUrlPatterns: Array[String] = {
    try {
      val patterns = NbBundle.getMessage(classOf[Query], "quicksearch.web.url_patterns" ) //NOI18N
      patterns.split("\\s|,|;|:" )
    } catch {case ex: MissingResourceException =>; null}
  }
}

class Query private () {
  import Query._
  
  private var searchThread: Thread = _
  private var searchOffset = 0
    
  def search(searchString: String): Result = {
    abort
    val res = new Result
    searchThread = new Thread(createSearch(searchString, res, 0))
    searchThread.start
    try {
      searchThread.join(20*1000)
    } catch {case ex: InterruptedException =>}

    res
  }
    
  def searchMore(searchString: String ): Result = {
    searchOffset += MAX_NUM_OF_RESULTS
    val res = new Result
    val searchMoreThread = new Thread(createSearch(searchString, res, searchOffset))
    searchMoreThread.start
    try {
      searchMoreThread.join(20 * 1000)
    } catch {case ex: InterruptedException =>}

    res
  }
    
  private def abort {
    if (null == searchThread) {
      return
    }
        
    searchOffset = 0
    searchThread.interrupt
    searchThread = null
  }
    
  private def createSearch(searchString: String, result: Result, searchOffset: Int): Runnable = {
    val res = new Runnable {
      def run() {
        var query = searchString
        query = query.replaceAll(" ", "+") //NOI18N //NOI18N
        query = query.replaceAll("#", "%23") //NOI18N //NOI18N
        query += "&num=" + MAX_NUM_OF_RESULTS //NOI18N
        query += "&hl=" + Locale.getDefault.getLanguage //NOI18N
        if (null != SITE_SEARCH )
          query += "&sitesearch=" + SITE_SEARCH //NOI18N
        if (searchOffset > 0 ) {
          query += "&start=" + searchOffset //NOI18N
        }
        try {
          val s = new Socket("google.com", 80) //NOI18N
          val p = new PrintStream(s.getOutputStream)
          p.print("GET /search?q=" + query + " HTTP/1.0\r\n") //NOI18N //NOI18N
          //fake browser headers
          p.print("User-Agent: Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.8.1) Gecko/20061010 Firefox/2.0\r\n") //NOI18N
          p.print("Connection: close\r\n\r\n") //NOI18N
          //TODO proxy
          val in = new InputStreamReader(s.getInputStream)

          val buffer = new BufferedReader(in)

          var line: String = null
          val rawHtml = new StringBuffer
          while ({line = buffer.readLine; line != null}) {
            rawHtml.append(line)
          }

          in.close

          result.parse(rawHtml.toString, searchOffset)
          result.filterUrl(URL_PATTERNS)
        } catch {case ex: IOException => Logger.getLogger(classOf[Query].getName).log(Level.INFO, null, ex)
        }
      }
    }

    res
  }
    
}
