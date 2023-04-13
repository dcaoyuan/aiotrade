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

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent
import java.awt.event.KeyEvent;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import javax.swing.text.JTextComponent;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle


/**
 * Quick search toolbar component
 * @author  Jan Becicka
 */
class QuickSearchComboBar(ks: KeyStroke) extends AbstractQuickSearchComboBar(ks) {
  import AbstractQuickSearchComboBar._

  private val jPanel1 = new javax.swing.JPanel
  private val jLabel2 = new javax.swing.JLabel
  private val jScrollPane1 = new javax.swing.JScrollPane
  private val jSeparator1 = new javax.swing.JSeparator

  val findIcon = new ImageIcon(getClass.getResource("/org/aiotrade/modules/quicksearch/resources/find.png")) // NOI18N

  initComponents

  /** Timer used for progress animation (see #143019).  */
  private val animationTimer = new Timer(100, new ActionListener {
      lazy val icons = {
        val x = new Array[ImageIcon](8)
        for (i <- 0 until 8) {
          x(i) = ImageUtilities.loadImageIcon("/org/aiotrade/modules/quicksearch/resources/progress_" + i + ".png", false)  //NOI18N
        }
        x
      }

      var index = 0
      def actionPerformed(e: ActionEvent) {
        jLabel2.setIcon(icons(index))
        index = (index + 1) % 8
      }
    })

  private def initComponents {
    setLayout(new java.awt.GridBagLayout())

    jPanel1.setBackground(getTextBackground)
    jPanel1.setBorder(javax.swing.BorderFactory.createLineBorder(getComboBorderColor))
    jPanel1.setName("jPanel1") // NOI18N
    jPanel1.setLayout(new java.awt.GridBagLayout)

    jLabel2.setIcon(findIcon)
    jLabel2.setToolTipText(NbBundle.getMessage(classOf[QuickSearchComboBar], "QuickSearchComboBar.jLabel2.toolTipText")) // NOI18N
    jLabel2.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1))
    jLabel2.setName("jLabel2") // NOI18N
    jLabel2.addMouseListener(new java.awt.event.MouseAdapter {
        override def mousePressed(evt: java.awt.event.MouseEvent) {
          jLabel2MousePressed(evt)
        }
      })

    var gridBagConstraints = new java.awt.GridBagConstraints
    gridBagConstraints.gridx = 0
    gridBagConstraints.gridy = 0
    gridBagConstraints.weighty = 1.0
    gridBagConstraints.insets = new java.awt.Insets(1, 2, 1, 1)
    jPanel1.add(jLabel2, gridBagConstraints)

    jScrollPane1.setBorder(null)
    jScrollPane1.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER)
    jScrollPane1.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER)
    jScrollPane1.setViewportBorder(null)
    jScrollPane1.setMinimumSize(new java.awt.Dimension(2, 18))
    jScrollPane1.setName("jScrollPane1") // NOI18N

    jScrollPane1.setViewportView(command)

    gridBagConstraints = new java.awt.GridBagConstraints
    gridBagConstraints.gridx = 2
    gridBagConstraints.gridy = 0
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH
    gridBagConstraints.weightx = 1.0
    gridBagConstraints.weighty = 1.0
    gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 2)
    jPanel1.add(jScrollPane1, gridBagConstraints)

    jSeparator1.setOrientation(javax.swing.SwingConstants.VERTICAL);
    jSeparator1.setName("jSeparator1") // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints
    gridBagConstraints.gridx = 1
    gridBagConstraints.gridy = 0
    gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL
    gridBagConstraints.weighty = 1.0
    gridBagConstraints.insets = new java.awt.Insets(3, 0, 3, 3)
    jPanel1.add(jSeparator1, gridBagConstraints)

    gridBagConstraints = new java.awt.GridBagConstraints
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL
    gridBagConstraints.weightx = 1.0
    add(jPanel1, gridBagConstraints)
  }

  private def jLabel2MousePressed(evt: java.awt.event.MouseEvent) {
    maybeShowPopup(evt)
  }

  override protected def createCommandField: JTextComponent = {
    val res = new DynamicWidthTA
    res.setRows(1)
    res.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1))
    // disable default Swing's Ctrl+Shift+O binding to enable our global action
    var curIm = res.getInputMap(JComponent.WHEN_FOCUSED)
    while (curIm != null) {
      curIm.remove(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK))
      curIm = curIm.getParent
    }
    res
  }

  override protected def innerComponent: JComponent = {
    jPanel1
  }

  def startProgressAnimation {
    if (animationTimer != null && !animationTimer.isRunning) {
      animationTimer.start
    }
  }

  def stopProgressAnimation {
    if (animationTimer != null && animationTimer.isRunning) {
      animationTimer.stop
      jLabel2.setIcon(findIcon)
    }
  }

  private final class DynamicWidthTA extends JTextArea {
    private lazy val prefWidth: Dimension = {
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
