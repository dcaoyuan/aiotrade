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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import org.openide.util.Utilities;

/**
 * ListCellRenderer for SearchResults
 * @author Jan Becicka
 */
object SearchResultRender {
  def getKeyStrokeAsText(keyStroke: KeyStroke): String = {
    if (keyStroke == null)  return ""

    val modifiers = keyStroke.getModifiers
    val sb = new StringBuffer
    if ((modifiers & InputEvent.CTRL_DOWN_MASK) > 0)
      sb append "Ctrl+"
    if ((modifiers & InputEvent.ALT_DOWN_MASK) > 0)
      sb append "Alt+"
    if ((modifiers & InputEvent.SHIFT_DOWN_MASK) > 0)
      sb append "Shift+"
    if ((modifiers & InputEvent.META_DOWN_MASK) > 0)
      if (Utilities.isMac) {
        // Mac cloverleaf symbol
        sb append "\u2318+"
      } else if (isSolaris) {
        // Sun meta symbol
        sb append "\u25C6+"
      } else {
        sb append "Meta+"
      }
    if (keyStroke.getKeyCode != KeyEvent.VK_SHIFT &&
        keyStroke.getKeyCode != KeyEvent.VK_CONTROL &&
        keyStroke.getKeyCode != KeyEvent.VK_META &&
        keyStroke.getKeyCode != KeyEvent.VK_ALT &&
        keyStroke.getKeyCode != KeyEvent.VK_ALT_GRAPH
    )
      sb append Utilities.keyToString(KeyStroke.getKeyStroke(keyStroke.getKeyCode, 0))

    sb.toString
  }

  private def isSolaris: Boolean = {
    val osName = System.getProperty ("os.name")
    osName != null && osName.startsWith ("SunOS")
  }


  /** Truncate text and put "..." at the end if text exceeds given JLabel
   * coordinates - workaround fo JLabel inability to truncate html
   */
  private def truncateLabel(label: JLabel) {
    val text = label.getText

    // no need to truncate non html text, JLabel will do it itself
    if (!text.startsWith("<html>")) {
      return
    }

    val prefWidth = label.getPreferredSize().width
    val curWidth = label.getWidth
    if (curWidth > 0 && prefWidth > curWidth) {
      // get rid of html, JLabel will then correctly put "..." at the end
      label.setText(text.replaceAll("<.*?>", ""))
    }

  }
}

class SearchResultRender(popup: QuickSearchPopup) extends JLabel with ListCellRenderer[ItemResult] {
  import SearchResultRender._
  
  private val IS_GTK = UIManager.getLookAndFeel.getID == "GTK" //NOI18N

  private var categoryLabel: JLabel = _
  private var rendererComponent: JPanel = _
  private var resultLabel, shortcutLabel: JLabel = _
  private var dividerLine: JPanel = _
  private var itemPanel: JPanel = _

  configRenderer

  def getListCellRendererComponent(list: JList[_ <: ItemResult], value: ItemResult, index: Int, isSelected: Boolean, cellHasFocus: Boolean): Component = {
    val ir = value
    val shortcut = ir.shortcut
    resultLabel.setText(ir.displayName)
    truncateLabel(resultLabel)
    if (shortcut != null && shortcut.size > 0 && !shortcut.isEmpty) {
      // TBD - display multi shortcuts
      shortcutLabel.setText(getKeyStrokeAsText(shortcut.head))
      itemPanel.add(shortcutLabel, BorderLayout.EAST)
    } else {
      itemPanel.remove(shortcutLabel)
    }

    val cr = ir.category
    if (cr.isFirstItem(ir)) {
      categoryLabel.setText(cr.category.displayName)
      if (index > 0) {
        rendererComponent.add(dividerLine, BorderLayout.NORTH)
      }
    } else {
      categoryLabel.setText("")
      rendererComponent.remove(dividerLine)
    }

    categoryLabel.setPreferredSize(new Dimension(popup.getCategoryWidth, categoryLabel.getPreferredSize.height))
    itemPanel.setPreferredSize(new Dimension(popup.getResultWidth, itemPanel.getPreferredSize.height))

    if (isSelected) {
      resultLabel   setBackground list.getSelectionBackground
      resultLabel   setForeground list.getSelectionForeground
      shortcutLabel setBackground list.getSelectionBackground
      shortcutLabel setForeground list.getSelectionForeground
    } else {
      resultLabel   setBackground AbstractQuickSearchComboBar.getResultBackground
      resultLabel   setForeground list.getForeground
      shortcutLabel setBackground AbstractQuickSearchComboBar.getResultBackground
      shortcutLabel setForeground list.getForeground
    }
    if( "Aqua" == UIManager.getLookAndFeel.getID) //NOI18N
      rendererComponent.setOpaque(false)

    rendererComponent
  }

  private def configRenderer {
    categoryLabel = new JLabel
    categoryLabel.setFont(categoryLabel.getFont.deriveFont(Font.BOLD))
    categoryLabel.setBorder(new EmptyBorder(0, 5, 0, 0))
    categoryLabel.setForeground(AbstractQuickSearchComboBar.getCategoryTextColor)

    resultLabel = new JLabel
    resultLabel.setOpaque(true)
    resultLabel.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4))

    shortcutLabel = new JLabel
    shortcutLabel.setOpaque(true)
    shortcutLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 4))

    itemPanel = new JPanel
    itemPanel.setBackground(AbstractQuickSearchComboBar.getResultBackground)
    itemPanel.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 3))
    itemPanel.setLayout(new BorderLayout)
    itemPanel.add(resultLabel, BorderLayout.CENTER)

    dividerLine = new JPanel
    dividerLine.setBackground(AbstractQuickSearchComboBar.getPopupBorderColor)
    dividerLine.setPreferredSize(new Dimension(dividerLine.getPreferredSize.width, 1))

    rendererComponent = new JPanel
    rendererComponent.setLayout(new BorderLayout)
    rendererComponent.add(itemPanel, BorderLayout.CENTER)
    rendererComponent.add(categoryLabel, BorderLayout.WEST)
  }


}
