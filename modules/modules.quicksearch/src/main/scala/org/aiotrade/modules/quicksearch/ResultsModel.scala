/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
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

package org.aiotrade.modules.quicksearch;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;
import javax.swing.AbstractListModel;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import org.aiotrade.spi.quicksearch.SearchRequest


/**
 * Model of search results. Works as ListModel for JList which is displaying
 * results. Actual results data are stored in List of CategoryResult objects.
 *
 * As model changes can come very frequently, firing of changes is coalesced.
 * Coalescing of changes helps UI to reduce flicker and unnecessary updates.
 *
 * @author Jan Becicka
 */
object ResultsModel extends AbstractListModel[ItemResult] with ActionListener {
  /** Amount of time during which model has to be unchanged in order to fire
   * changes to listeners. */
  val COALESCE_TIME = 200

  private var _content: List[_ <: CategoryResult] = _

  /* Timer for coalescing fast coming changes of model */
  private var fireTimer: Timer = _

  def content = _content.toList
  def content_=(categories: List[_ <: CategoryResult]) {
    val oldRes = this._content
    this._content = categories

    if (oldRes != null) {
      for (cr <- oldRes) {
        cr.setObsolete(true)
      }
    }

    maybeFireChanges
  }

  /******* AbstractListModel impl ********/

  def getSize: Int = {
    if (_content == null) {
      return 0
    }
    var size = 0
    for (cr <- _content) {
      size += cr.items.size
    }
    size
  }

  def getElementAt(index: Int): ItemResult = {
    if (_content == null) {
      return null
    }
    // TBD - should probably throw AIOOBE if invalid index is on input
    var catIndex = index
    for (cr <- _content) {
      val catItems = cr.items
      val catSize = catItems.size
      if (catIndex < catSize) {
        return if (catIndex >= 0) catItems(catIndex) else null
      }
      catIndex -= catSize
    }
    
    null
  }

  def categoryChanged(cr: CategoryResult) {
    // fire change only if category is contained in model
    if (_content != null && _content.contains(cr)) {
      maybeFireChanges
    }
  }

  private def maybeFireChanges {
    if (fireTimer == null) {
      fireTimer = new Timer(COALESCE_TIME, this)
    }
    if (!fireTimer.isRunning) {
      // first change in possible flurry, start timer
      fireTimer.start
    } else {
      // model change came too fast, let's wait until providers calm down :)
      fireTimer.restart
    }
  }

  def actionPerformed(e: ActionEvent) {
    fireTimer.stop
    fireContentsChanged(this, 0, getSize)
  }

}

final case class ItemResult(category: CategoryResult, private val sRequest: SearchRequest, action: Runnable,
                            private val $displayName: String,
                            shortcut: List[_ <: KeyStroke], displayHint: String,
                            //time of last access, used for recent searches
                            var date: Date) {

  private val HTML = "<html>"

  val displayName = if (sRequest != null) highlightSubstring($displayName, sRequest) else $displayName

  def this(category: CategoryResult, sRequest: SearchRequest, action: Runnable, displayName: String) = {
    this(category, sRequest, action, displayName, null, null, null)
  }

  def this(category: CategoryResult, action: Runnable, displayName: String, date: Date) = {
    this(category, null, action, displayName, null, null, date)
  }

  def this(category: CategoryResult, sRequest: SearchRequest, action: Runnable, displayName: String,
           shortcut: List[_ <: KeyStroke], displayHint: String) = {
    this(category, sRequest, action, displayName, shortcut, displayHint, null)
  }

  private def highlightSubstring(text: String, sRequest: SearchRequest): String = {
    if (text.startsWith(HTML)) {
      // provider handles highliting itself, okay
      return text
    }
    // try to find substring
    val searchedText = sRequest.text
    val index = text.toLowerCase.indexOf(searchedText.toLowerCase)
    if (index == -1) {
      return text
    }
    // found, bold it
    val endIndex = index + searchedText.length
    val sb = new StringBuilder(HTML)
    if (index > 0) {
      sb.append(text.substring(0, index))
    }
    sb.append("<b>")
    sb.append(text.substring(index, endIndex))
    sb.append("</b>")
    if (endIndex < text.length) {
      sb.append(text.substring(endIndex, text.length))
    }

    sb.toString
  }
}