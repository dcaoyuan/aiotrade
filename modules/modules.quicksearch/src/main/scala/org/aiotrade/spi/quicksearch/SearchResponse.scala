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

package org.aiotrade.spi.quicksearch;

import javax.swing.KeyStroke;
import org.aiotrade.modules.quicksearch.CategoryResult;
import org.aiotrade.modules.quicksearch.ItemResult;
    
/**
 * Response object for collecting results of {@link SearchProvider#evaluate} search
 * operation. SearchProvider implementors are expected to fill SearchResponse
 * in steps by calling various {@link SearchResponse#addResult} methods.
 * 
 * @author Dafe Simonek
 */
class SearchResponse(catResult: CategoryResult, sRequest: SearchRequest) {

  /** Package private creation, made available to other packages via
   * Accessor pattern.
   * @param catResult CategoryResult for storing response data
   */

  /**
   * Adds new result of quick search operation.
   *
   * @param action Runnable to invoke when this result item is chosen by user.
   * Providers are expected to signal unsuccessful invocation of <code>Runnable.run</code>
   * by writing into status line and producing beep. Invocation failures may happen,
   * as <code>Runnable.run</code> may be called later, when conditions or context
   * changed in a way that action can't be performed.<p></p>
   *
   * @param htmlDisplayName Localized display name of this result item. Note
   * that <b>&lt;b&gt;</b> and <b>&lt;/b&gt;</b> html tags should be used to emphasize part of the result.
   * Common provider implementations will use bold marking for found substring, so
   * resulting string should look like <b>"Item containing &lt;b&gt;searched&lt;/b&gt; text"</b>, where
   * "searched" is text returned from {@link SearchRequest#getText()}.<br></br>
   * It's possible but not recommended to use other basic html tags, as readability
   * of results may suffer.<p></p>
   *
   * @return true when result was accepted and more results are needed if available.
   * False when no further results are needed.
   * {@link SearchProvider} implementors should stop computing and leave
   * SearchProvider.evaluate(...) immediatelly if false is returned.
   */
  def addResult(action: Runnable, htmlDisplayName: String): Boolean = {
    addResult(action, htmlDisplayName, null, null);
  }
    
  /**
   * Adds new result of quick search operation.
   *
   * @param action Runnable to invoke when this result item is chosen by user.
   * Providers are expected to signal unsuccessful invocation of <code>Runnable.run</code>
   * by writing into status line and producing beep. Invocation failures may happen,
   * as <code>Runnable.run</code> may be called later, when conditions or context
   * changed in a way that action can't be performed.<p></p>
   *
   * @param htmlDisplayName Localized display name of this result item. Note
   * that <b>&lt;b&gt;</b> and <b>&lt;/b&gt;</b> html tags should be used to emphasize part of the result.
   * Common provider implementations will use bold marking for found substring, so
   * resulting string should look like <b>"Item containing &lt;b&gt;searched&lt;/b&gt; text"</b>, where
   * "searched" is text returned from {@link SearchRequest#getText()}.<br></br>
   * It's possible but not recommended to use other basic html tags, as readability
   * of results may suffer.<p></p>
   *
   * @param displayHint Localized display hint of this result item or null if not available<p></p>
   *
   * @param shortcut Shortcut of this result item or null if shorcut isn't available<p></p>
   *
   * @return true when result was accepted and more results are needed if available.
   * False when no further results are needed.
   * {@link SearchProvider} implementors should stop computing and leave
   * SearchProvider.evaluate(...) immediatelly if false is returned.
   */
  def addResult(action: Runnable, htmlDisplayName: String,
                displayHint: String, shortcut: List[_ <: KeyStroke]): Boolean = {
    catResult.addItem(new ItemResult(catResult, sRequest, action, htmlDisplayName, shortcut, displayHint))
  }

}
