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

import java.awt.Toolkit;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.aiotrade.spi.quicksearch.SearchProvider;
import org.aiotrade.spi.quicksearch.SearchRequest;
import org.aiotrade.spi.quicksearch.SearchResponse;
import org.openide.awt.HtmlBrowser;
import org.openide.awt.StatusDisplayer;
import org.openide.util.NbBundle;

/**
 *
 * @author S. Aubrecht
 */
object WebQuickSearchProviderImpl {

  private def createAction(url: String): Runnable = {
    new Runnable {
      def run {
        val extendedUrl = appendId(url)
        try {
          val displayer = HtmlBrowser.URLDisplayer.getDefault
          if (displayer != null) {
            displayer.showURL(new URL(extendedUrl))
          }
        } catch {case ex: Exception =>
            StatusDisplayer.getDefault.setStatusText(
              NbBundle.getMessage(classOf[WebQuickSearchProviderImpl], "Err_CannotDisplayURL", extendedUrl)
            ) //NOI18N
            Toolkit.getDefaultToolkit.beep
            Logger.getLogger(classOf[WebQuickSearchProviderImpl].getName).log(Level.FINE, null, ex)
        }
      }
    }
  }

  private def appendId(url: String): String = {
    val sb = new StringBuffer(url)
    if (url.contains("?") ) { //NOI18N
      sb.append('&') //NOI18N
    } else {
      sb.append('?') //NOI18N
    }
    sb.append("cid=925878")
    sb.toString
  }
}

class WebQuickSearchProviderImpl extends SearchProvider {
  import WebQuickSearchProviderImpl._

  private lazy val query = Query()
    
  def evaluate(request: SearchRequest, response: SearchResponse) {
    var res = query.search(request.text)
    do {
      for (item <- res.getItems) {
        if (!response.addResult(createAction(item.url), item.title))
          return
      }
      res = query.searchMore( request.text)
    } while (!res.isSearchFinished)
  }

}
