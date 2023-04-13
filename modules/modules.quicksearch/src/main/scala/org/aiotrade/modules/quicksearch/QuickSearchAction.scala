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

import java.awt.KeyEventDispatcher
import java.awt.KeyboardFocusManager
import java.awt.event.KeyEvent
import javax.swing.Action
import javax.swing.KeyStroke
import javax.swing.UIManager
import javax.swing.text.JTextComponent
import org.openide.util.HelpCtx
import org.openide.util.NbBundle
import org.openide.util.actions.CallableSystemAction

/**
 * QuickSearch Action provides toolbar presenter
 * @author  Jan Becicka
 */
class QuickSearchAction extends CallableSystemAction {

  private val isAqua = UIManager.getLookAndFeel.getID == "Aqua"
  private val comboBar: AbstractQuickSearchComboBar = if (isAqua)
    new AquaQuickSearchComboBar(getValue(Action.ACCELERATOR_KEY).asInstanceOf[KeyStroke])
  else
    new QuickSearchComboBar(getValue(Action.ACCELERATOR_KEY).asInstanceOf[KeyStroke])

  // --- global keyboard processing
  private val kfm = KeyboardFocusManager.getCurrentKeyboardFocusManager
  private val dispatcher = new KeyEventDispatcher {
    def dispatchKeyEvent(e: KeyEvent): Boolean = {
      if (e.getID == KeyEvent.KEY_TYPED && !e.isAltDown && !e.isControlDown && !e.isMetaDown) {
        val c = e.getKeyChar
        if (c >= 'A' && c <= 'z' || c >= '0' && c <= '9') {
          if (!kfm.getFocusOwner.isInstanceOf[JTextComponent]) {
            comboBar.requestFocus
            // let comboBar's focusGained event process this char
            comboBar.capturedChar = c
            // return true -- dispatched
            return true 
          }
        }
      }

      false
    }
  }
  kfm.addKeyEventDispatcher(dispatcher)

  def performAction {
    if (comboBar.command.isFocusOwner) {
      // repetitive action invocation, reset search to all categories
      comboBar.evaluateCategory(null, false)
    } else {
      comboBar.requestFocus
    }
  }

  def getName = {
    NbBundle.getMessage(classOf[QuickSearchAction], "CTL_QuickSearchAction")
  }

  override protected def iconResource = {
    "org/aiotrade/modules/quicksearch/resources/edit_parameters.png"
  }

  def getHelpCtx: HelpCtx = {
    HelpCtx.DEFAULT_HELP
  }

  override protected def asynchronous = {
    false
  }

  override def getToolbarPresenter: java.awt.Component = {
    comboBar
  }

}
