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

package org.aiotrade.modules.quicksearch.recent;

import java.util.Calendar
import java.util.Date;
import java.util.prefs.Preferences;
import org.aiotrade.modules.quicksearch.CommandEvaluator;
import org.aiotrade.modules.quicksearch.ResultsModel;
import org.aiotrade.modules.quicksearch.ItemResult;
import org.openide.util.Exceptions;
import org.openide.util.NbPreferences;
import scala.collection.mutable.ArrayBuffer



/**
 * Recent Searches items storage and its persistance
 *
 * @author Jan Becicka
 * @authoe Max Sauer
 */
object RecentSearches {
  private lazy val instance = new RecentSearches
  def apply() = instance
}
class RecentSearches private () {
    
  private val MAX_ITEMS = 5
  private val FIVE_DAYS = 86400000 * 5
  
  private val recent = new ArrayBuffer[ItemResult] {
    override def toString: String = {
      val sb = new StringBuffer
      for (td <- this) {
        sb.append(td.displayName + ":" + td.date.getTime + ",")
      }
      sb.toString
    }

  }
  
  private val RECENT_SEARCHES = "recentSearches" //preferences
  readRecentFromPrefs //read recent searhces from preferences
    
  def add(result: ItemResult) {
    val now = Calendar.getInstance.getTime

    // don't create duplicates, however poor-man's test only
    for (ir <- recent) {
      if (stripHTMLnames(ir.displayName) == stripHTMLnames(result.displayName)) {
        ir.date = now
        return
      }
    }

    // ugly hack to not include special Maven setup search item
    if ("SearchSetup" == result.action.getClass.getSimpleName) {
      return
    }
        
    if (recent.size >= MAX_ITEMS) {
      recent.remove(recent.size - 1)
    }
    result.date = now
    recent.insert(0, result)
    prefs.put(RECENT_SEARCHES, stripHTMLnames(recent.toString))
  }
    
  def getSearches: List[ItemResult] = {
    var fiveDayList = ArrayBuffer[ItemResult]()
    for (ir <- recent) {
      if ((Calendar.getInstance.getTime.getTime - ir.date.getTime) < FIVE_DAYS)
        fiveDayList += ir
    }
    //provide only recent searches newer than five days
    fiveDayList.toList
  }

  private def prefs: Preferences = {
    NbPreferences.forModule(classOf[RecentSearches])
  }

  private def readRecentFromPrefs {
    val items = prefs.get(RECENT_SEARCHES, "").split(",") // NOI18N
    if (items(0).length != 0) {
      for (i <- 0 until items.length) {
        val semicolonPos = items(i).lastIndexOf(":") // NOI18N
        if (semicolonPos >= 0) {
          val name = items(i).substring(0, semicolonPos)
          val time = items(i).substring(semicolonPos + 1).toLong
          val incomplete = new ItemResult(null, new FakeAction(name), name, new Date(time))
          recent += incomplete
        }
      }
    }
  }

  private def stripHTMLnames(s: String): String = {
    s.replaceAll("<.*?>", "").trim
  }

  /**
   * Lazy initied action used for recent searches
   * In order to not init all recent searched item
   * @param name display name to search for
   */
  final class FakeAction(name: String) extends Runnable {

    private var action: Runnable = _ //remembered action

    def run {
      if (action == null || action.isInstanceOf [FakeAction]) {
        val model = ResultsModel
        CommandEvaluator.evaluate(stripHTMLandPackageNames(name))
        try {
          Thread.sleep(350)
        } catch {case ex: InterruptedException => Exceptions.printStackTrace(ex)}
        
        val rSize = model.getSize
        var i = 0
        var break = false
        while (i < rSize && !break) {
          val res = model.getElementAt(i)
          if (stripHTMLnames(res.displayName) == stripHTMLnames(name)) {
            action = res.action
            if (!action.isInstanceOf[FakeAction]) {
              action.run
              break = true
            }
          }
          i += 1
        }
      } else {
        action.run
      }
    }

    private def stripHTMLandPackageNames(s: String): String = {
      stripHTMLnames(s).replaceAll("\\(.*\\)", "").trim
    }
  }

}
