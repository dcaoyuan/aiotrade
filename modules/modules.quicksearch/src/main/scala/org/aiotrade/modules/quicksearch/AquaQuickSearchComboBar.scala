
/**
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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.JTextComponent;

/**
 * Quick search toolbar component
 * @author  Jan Becicka
 */
class AquaQuickSearchComboBar(ks: KeyStroke) extends AbstractQuickSearchComboBar(ks) {

  setLayout(new BorderLayout)
  add(command, BorderLayout.CENTER)

  override protected def createCommandField: JTextComponent = {
    val res = new DynamicWidthTF
    val dummy = new JPopupMenu
    dummy.addPopupMenuListener( new PopupMenuListener {
        def popupMenuWillBecomeVisible(e: PopupMenuEvent) {
          SwingUtilities.invokeLater(new Runnable {
              def run {
                dummy.setVisible(false)
              }
            })
        }

        def popupMenuWillBecomeInvisible(e: PopupMenuEvent) {
        }

        def popupMenuCanceled(e: PopupMenuEvent) {
        }
      })

    res.putClientProperty("JTextField.variant", "search")
    res.putClientProperty("JTextField.Search.FindPopup", dummy)
    res.putClientProperty("JTextField.Search.FindAction", new ActionListener {
        def actionPerformed(e: ActionEvent) {
          maybeShowPopup(null)
        }
      })

    res
  }

  override protected def innerComponent: JComponent = {
    command
  }

  private final class DynamicWidthTF extends JTextField {
    private lazy val prefWidth = {
      val orig = super.getPreferredSize
      new Dimension(computePrefWidth, orig.height)
    }

    override def getPreferredSize: Dimension = {
      prefWidth
    }

    override def getMinimumSize: Dimension = {
      getPreferredSize
    }
  }

}
