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

package org.aiotrade.modules.quicksearch

import java.awt.EventQueue
import javax.swing.SwingUtilities
import org.openide.util.NbBundle
import scala.collection.mutable.ArrayBuffer


/**
 * Thread safe model of provider results of asociated category.
 * 
 * @author  Jan Becicka, Dafe Simonek
 */
class CategoryResult(val category: ProviderModel.Category, allResults: Boolean) extends Runnable {
    
  private val MAX_RESULTS = 20
  private val ALL_MAX_RESULTS = 30

  private val LOCK = new Object
    
  private val _items = new ArrayBuffer[ItemResult](if (allResults) ALL_MAX_RESULTS else MAX_RESULTS)
    
  private var obsolete = false
  private var previousSize = 0
  private var moreResults = false

  def addItem(item: ItemResult): Boolean = {
    LOCK synchronized {
      if (obsolete) {
        return false
      }
      if (_items.size >= (if (allResults) ALL_MAX_RESULTS else MAX_RESULTS)) {
        if (!allResults) {
          moreResults = true
        }
        return false
      }
      _items += item
    }
        
    if (EventQueue.isDispatchThread) {
      run
    } else {
      SwingUtilities.invokeLater(this)
    }
        
    true
  }
    
  /**
   * Get the value of item
   *
   * @return the value of item
   */
  def items: List[ItemResult] = {
    var rItems = List[ItemResult]()
    LOCK synchronized  {
      rItems = _items.toList
      if (moreResults) {
        rItems ::= new ItemResult(this, null, this, NbBundle.getMessage(getClass, "LBL_MoreResults"))
      }
    }

    rItems
  }
    
  def isFirstItem(ir: ItemResult): Boolean = {
    LOCK synchronized {
      if (_items.size > 0 && _items(0).equals(ir)) {
        return true
      }
    }

    false
  }

  def setObsolete(obsolete: Boolean) {
    LOCK synchronized {
      this.obsolete = obsolete
    }
  }

  def isObsolete = {
    obsolete
  }

  /** Sends notification about category change, always runs in EQ thread */
  def run {
    var curSize = 0
    var shouldNotify = false
    LOCK synchronized {
      curSize = _items.size
      shouldNotify = !obsolete && _items.size <= (if (allResults) ALL_MAX_RESULTS else MAX_RESULTS)
    }
        
    if (!shouldNotify) {
      return
    }

    // as this method is called later then data change occurred (invocation through SwingUtilities.invokeLater),
    // it happens that all data are already added when this code is executed,
    // especially when provider is fast calling addItem. In such situation,
    // notification changes are redundant. We can get rid of them by controlling
    // category size. Data are only added through addItem, never removed,
    // and so the same size also means the same content and change
    // notification may be dismissed.
    if (curSize > previousSize) {
      previousSize = curSize
      ResultsModel.categoryChanged(this)
    }
  }

}
