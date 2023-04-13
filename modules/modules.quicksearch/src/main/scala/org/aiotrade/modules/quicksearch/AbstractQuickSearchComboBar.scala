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

import java.awt.Color;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent;
import java.lang.ref.WeakReference;
import javax.swing.JComponent;
import javax.swing.JPanel
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import org.aiotrade.modules.quicksearch.ProviderModel.Category
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import org.openide.windows.TopComponent;

/**
 * Quick search toolbar component
 * @author  Jan Becicka
 */
object AbstractQuickSearchComboBar {

  def isAqua = UIManager.getLookAndFeel.getID == "Aqua"

  def getComboBorderColor: Color = {
    val shadow = if (Utilities.isWindows) {
      UIManager.getColor("Nb.ScrollPane.Border.color")
    } else {
      UIManager.getColor("TextField.shadow")
    }

    if (shadow != null) shadow else getPopupBorderColor
  }

  def getPopupBorderColor: Color = {
    val shadow = UIManager.getColor("controlShadow")

    if (shadow != null) shadow else Color.GRAY
  }

  def getTextBackground: Color = {
    val textB = if (isAqua) {
      UIManager.getColor("NbExplorerView.background") //NOI18N
    } else {
      UIManager.getColor("TextPane.background")
    }

    if (textB != null) textB else Color.WHITE
  }

  def getResultBackground: Color = {
    getTextBackground
  }

  def getCategoryTextColor: Color = {
    val shadow = if (isAqua) {
      UIManager.getColor("Table.foreground")
    } else {
      UIManager.getColor("textInactiveText")
    }

    if (shadow != null) shadow else Color.DARK_GRAY
  }
}

abstract class AbstractQuickSearchComboBar(val keyStroke: KeyStroke) extends JPanel with ActionListener {

  private val CATEGORY_PROP = "cat"

  val command = createCommandField
  val displayer = new QuickSearchPopup(this)

  var caller: WeakReference[TopComponent] = _
  var origForeground: Color = _
  var capturedChar: Char = 0

  initComponents

  setShowHint(true)

  command.getDocument.addDocumentListener(new DocumentListener {
      def insertUpdate(arg0: DocumentEvent) {
        textChanged
      }

      def removeUpdate(arg0: DocumentEvent) {
        textChanged
      }

      def changedUpdate(arg0: DocumentEvent) {
        textChanged
      }

      private def textChanged {
        if (command.isFocusOwner) {
          displayer.maybeEvaluate(command.getText)
        }
      }
    })

  protected def createCommandField: JTextComponent

  protected def innerComponent: JComponent

  private def initComponents {
    setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1))
    setMaximumSize(new java.awt.Dimension(200, 2147483647))
    setName("Form") // NOI18N
    setOpaque(false)
    addFocusListener(new FocusAdapter {
        override def focusLost(evt: FocusEvent) {
          formFocusLost(evt)
        }
      })

    command.setToolTipText(NbBundle.getMessage(classOf[AbstractQuickSearchComboBar], 
                                               "AbstractQuickSearchComboBar.command.toolTipText",
                                               Array[Object]("(" + SearchResultRender.getKeyStrokeAsText(keyStroke) + ")"))) // NOI18N
    command.setName("command") // NOI18N
    command.addFocusListener(new FocusAdapter {
        override def focusGained(evt: FocusEvent) {
          commandFocusGained(evt)
        }
        override def focusLost(evt: FocusEvent) {
          commandFocusLost(evt)
        }
      })
    command.addKeyListener(new java.awt.event.KeyAdapter {
        override def keyPressed(evt: KeyEvent) {
          commandKeyPressed(evt)
        }
      })
  }

  private def formFocusLost(evt: FocusEvent) {
    displayer.setVisible(false)
  }

  private def commandKeyPressed(evt: KeyEvent) {
    evt.getKeyCode match {
      case KeyEvent.VK_DOWN =>
        displayer.selectNext
        evt.consume
      case  KeyEvent.VK_UP =>
        displayer.selectPrev
        evt.consume
      case KeyEvent.VK_ENTER =>
        evt.consume
        invokeSelectedItem
      case KeyEvent.VK_ESCAPE =>
        returnFocus(true)
        displayer.clearModel
      case KeyEvent.VK_F10 if evt.isShiftDown =>
        maybeShowPopup(null)
      case _ =>
    }
  }

  /** Actually invokes action selected in the results list */
  def invokeSelectedItem {
    val list = displayer.getList
    val ir = list.getSelectedValue

    // special handling of invocation of "more results item" (three dots)
    if (ir != null) {
      ir.action match {
        case action: CategoryResult =>
          evaluateCategory(action.category, true)
          return
        case _ =>
      }
    }

    // #137259: invoke only some results were found
    if (list.getModel.getSize > 0) {
      returnFocus(false)
      // #137342: run action later to let focus indeed be transferred
      // by previous returnFocus() call
      SwingUtilities.invokeLater(new Runnable {
          def run {
            displayer.invoke
          }
        })
    }
  }

  private def returnFocus(force: Boolean) {
    displayer.setVisible(false)
    if (caller != null) {
      val tc = caller.get
      if (tc != null) {
        tc.requestActive
        tc.requestFocus
        return
      }
    }
    if (force) {
      KeyboardFocusManager.getCurrentKeyboardFocusManager.clearGlobalFocusOwner
    }
  }


  private def commandFocusLost(evt: FocusEvent) {
    displayer.setVisible(false)
    setShowHint(true)
  }

  private def commandFocusGained(evt: FocusEvent) {
    caller = new WeakReference[TopComponent](TopComponent.getRegistry.getActivated)
    setShowHint(false)
    if (CommandEvaluator.isCatTemporary) {
      CommandEvaluator.isCatTemporary = false
      CommandEvaluator.evalCat = null
    }
    
    if (capturedChar != 0) {
      command.getDocument.insertString(0, capturedChar.toString, null)
      capturedChar = 0
    }
  }

  protected def maybeShowPopup(evt: MouseEvent) {
    if (evt != null && !SwingUtilities.isLeftMouseButton(evt)) {
      return
    }

    val pm = new JPopupMenu
    val evalCat = if (!CommandEvaluator.isCatTemporary) {
      CommandEvaluator.evalCat
    } else null

    val allCats = new JRadioButtonMenuItem(NbBundle.getMessage(getClass, "LBL_AllCategories"), evalCat == null)
    allCats.addActionListener(this)
    pm.add(allCats)

    for (cat <- ProviderModel.categories if !CommandEvaluator.RECENT.equals(cat.name)) {
      val item = new JRadioButtonMenuItem(cat.displayName, cat == evalCat)
      item.putClientProperty(CATEGORY_PROP, cat)
      item.addActionListener(this)
      pm.add(item)
    }

    pm.show(innerComponent, 0, innerComponent.getHeight - 1)
  }

  /** ActionListener implementation, reaction to popup menu item invocation */
  def actionPerformed(e: ActionEvent) {
    val item = e.getSource.asInstanceOf[JRadioButtonMenuItem]
    CommandEvaluator.evalCat = item.getClientProperty(CATEGORY_PROP).asInstanceOf[Category]
    CommandEvaluator.isCatTemporary = false
    // refresh hint
    setShowHint(!command.isFocusOwner)
  }

  /** 
   * Runs evaluation narrowed to specified category
   */
  def evaluateCategory(cat: Category, temporary: Boolean) {
    CommandEvaluator.evalCat = cat
    CommandEvaluator.isCatTemporary = temporary
    displayer.maybeEvaluate(command.getText)
  }

  def setNoResults(areNoResults: Boolean) {
    // no op when called too soon
    if (command == null || origForeground == null) {
      return
    }
    // don't alter color if showing hint already
    if (command.getForeground.equals(command.getDisabledTextColor)) {
      return
    }
    command.setForeground(if (areNoResults) Color.RED else origForeground)
  }

  private def setShowHint(showHint: Boolean) {
    // remember orig color on first invocation
    if (origForeground == null) {
      origForeground = command.getForeground
    }
    if (showHint) {
      command.setForeground(command.getDisabledTextColor)
      val evalCat = CommandEvaluator.evalCat
      if (evalCat != null && !CommandEvaluator.isCatTemporary) {
        command.setText(getHintText(evalCat))
      } else {
        command.setText(getHintText(null))
      }
    } else {
      command.setForeground(origForeground)
      command.setText("")
    }
  }

  private def getHintText(cat: Category): String = {
    val sb = new StringBuilder
    if (cat != null) {
      sb.append(NbBundle.getMessage(classOf[AbstractQuickSearchComboBar], "MSG_DiscoverabilityHint2", cat.displayName)) //NOI18N
    } else {
      sb.append(NbBundle.getMessage(classOf[AbstractQuickSearchComboBar], "MSG_DiscoverabilityHint")) //NOI18N
    }
    sb.append(" (")
    sb.append(SearchResultRender.getKeyStrokeAsText(keyStroke))
    sb.append(")")

    sb.toString
  }


  override def requestFocus {
    super.requestFocus
    command.requestFocus
  }

  def getBottomLineY: Int = {
    innerComponent.getY + innerComponent.getHeight
  }

  protected def computePrefWidth: Int = {
    val fm = command.getFontMetrics(command.getFont)
    var maxWidth = 0
    for (cat <- ProviderModel.categories if cat.name != CommandEvaluator.RECENT) { // skip recent category
        maxWidth = math.max(maxWidth, fm.stringWidth(getHintText(cat)))
    }
    // don't allow width grow too much
    math.min(350, maxWidth)
  }
}
