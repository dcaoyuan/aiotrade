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

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import javax.swing.BorderFactory
import javax.swing.JComponent;
import javax.swing.JLabel
import javax.swing.JLayeredPane;
import javax.swing.JList;
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JSeparator
import javax.swing.ScrollPaneConstants
import javax.swing.SwingConstants
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import org.aiotrade.modules.quicksearch.recent.RecentSearches;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.util.Task;
import org.openide.util.TaskListener;

/**
 * Component representing drop down for quick search
 * @author  Jan Becicka
 */
class QuickSearchPopup(comboBar: AbstractQuickSearchComboBar) extends JPanel
                                                                 with ListDataListener
                                                                 with ActionListener
                                                                 with TaskListener
                                                                 with Runnable {
  private val jScrollPane1 = new JScrollPane
  private val jList1 = new JList[ItemResult]
  private val statusPanel = new JPanel
  private val searchingSep = new JSeparator
  private val searchingLabel = new JLabel
  private val noResultsLabel = new JLabel
  private val hintSep = new JSeparator
  private val hintLabel = new JLabel

  /* Rect to store repetitive bounds computation */
  private val popupBounds = new Rectangle

  private val COALESCE_TIME = 200
  private lazy val updateTimer: Timer = new Timer(COALESCE_TIME, this)

  /** text to search for */
  private var searchedText: String = _

  private var catWidth = 0
  private var resultWidth = 0
  private var evalTask: Task = _

  /** Creates new form SilverPopup */
  initComponents

  jList1.setModel(ResultsModel)
  jList1.setCellRenderer(new SearchResultRender(this))
  ResultsModel.addListDataListener(this)

  if (UIManager.getLookAndFeel.getID == "Aqua") {//NOI18N
    jList1.setBackground(AbstractQuickSearchComboBar.getResultBackground)
  }
  
  updateStatusPanel

  def invoke {
    val result = jList1.getModel.getElementAt(jList1.getSelectedIndex)
    if (result != null) {
      RecentSearches().add(result)
      result.action.run
      clearModel
    }
  }

  def selectNext {
    val oldSel = jList1.getSelectedIndex
    if (oldSel >= 0 && oldSel < jList1.getModel.getSize - 1) {
      jList1.setSelectedIndex(oldSel + 1)
    }
  }

  def selectPrev {
    val oldSel = jList1.getSelectedIndex
    if (oldSel > 0) {
      jList1.setSelectedIndex(oldSel - 1)
    }
  }

  def getList: JList[ItemResult] = {
    jList1
  }

  def clearModel {
    ResultsModel.content = null
  }

  def maybeEvaluate(text: String) {
    this.searchedText = text

    if (!updateTimer.isRunning) {
      // first change in possible flurry, start timer
      updateTimer.start
    } else {
      // text change came too fast, let's wait until user calms down :)
      updateTimer.restart
    }
  }

  /** implementation of ActionListener, called by timer,
   * actually runs search */
  def actionPerformed(e: ActionEvent) {
    updateTimer.stop
    // search only if we are not cancelled already
    if (comboBar.command.isFocusOwner) {
      if (evalTask != null) {
        evalTask.removeTaskListener(this)
      }
      evalTask = CommandEvaluator.evaluate(searchedText)
      evalTask.addTaskListener(this)
      // start waiting on all providers execution
      RequestProcessor.getDefault.post(evalTask)
    }
  }


  /** This method is called from within the constructor to
   * initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is
   * always regenerated by the Form Editor.
   */
  @unchecked
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private def initComponents {

    setBorder(BorderFactory.createLineBorder(AbstractQuickSearchComboBar.getPopupBorderColor))
    setLayout(new java.awt.BorderLayout)

    jScrollPane1.setBorder(null)
    jScrollPane1.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER)
    jScrollPane1.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER)

    jList1.setFocusable(false);
    jList1.addMouseListener(new MouseAdapter {
        override def mouseClicked(evt: MouseEvent) {
          jList1MouseClicked(evt)
        }
      })
    jList1.addMouseMotionListener(new MouseMotionAdapter {
        override def mouseMoved(evt: MouseEvent) {
          jList1MouseMoved(evt)
        }
      })
    jScrollPane1.setViewportView(jList1)

    add(jScrollPane1, java.awt.BorderLayout.CENTER)

    statusPanel.setBackground(AbstractQuickSearchComboBar.getResultBackground)
    statusPanel.setLayout(new java.awt.GridBagLayout)
    var gridBagConstraints = new java.awt.GridBagConstraints
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH
    gridBagConstraints.weightx = 1.0
    statusPanel.add(searchingSep, gridBagConstraints)

    searchingLabel.setText(NbBundle.getMessage(classOf[QuickSearchPopup], "QuickSearchPopup.searchingLabel.text")) // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints
    gridBagConstraints.gridx = 0
    gridBagConstraints.gridy = 1
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST
    statusPanel.add(searchingLabel, gridBagConstraints)

    noResultsLabel.setForeground(java.awt.Color.red)
    noResultsLabel.setHorizontalAlignment(SwingConstants.CENTER);
    noResultsLabel.setText(NbBundle.getMessage(classOf[QuickSearchPopup], "QuickSearchPopup.noResultsLabel.text")) // NOI18N
    noResultsLabel.setFocusable(false)
    gridBagConstraints = new java.awt.GridBagConstraints
    gridBagConstraints.gridx = 0
    gridBagConstraints.gridy = 3
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL
    gridBagConstraints.weightx = 1.0
    statusPanel.add(noResultsLabel, gridBagConstraints)
    gridBagConstraints = new java.awt.GridBagConstraints
    gridBagConstraints.gridx = 0
    gridBagConstraints.gridy = 4
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH
    gridBagConstraints.weightx = 1.0
    statusPanel.add(hintSep, gridBagConstraints)

    hintLabel.setBackground(AbstractQuickSearchComboBar.getResultBackground)
    hintLabel.setHorizontalAlignment(SwingConstants.CENTER)
    gridBagConstraints = new java.awt.GridBagConstraints
    gridBagConstraints.gridx = 0
    gridBagConstraints.gridy = 5
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH
    statusPanel.add(hintLabel, gridBagConstraints)

    add(statusPanel, java.awt.BorderLayout.PAGE_END)
  }// </editor-fold>//GEN-END:initComponents

  private def jList1MouseMoved(evt: MouseEvent) {//GEN-FIRST:event_jList1MouseMoved
    // selection follows mouse move
    val loc = evt.getPoint
    val index = jList1.locationToIndex(loc)
    if (index == -1) {
      return
    }
    val rect = jList1.getCellBounds(index, index)
    if (rect != null && rect.contains(loc)) {
      jList1.setSelectedIndex(index)
    }

  }//GEN-LAST:event_jList1MouseMoved

  private def jList1MouseClicked(evt: MouseEvent) {//GEN-FIRST:event_jList1MouseClicked
    if (!SwingUtilities.isLeftMouseButton(evt)) {
      return
    }
    // mouse left button click works the same as pressing Enter key
    comboBar.invokeSelectedItem

  }//GEN-LAST:event_jList1MouseClicked





  /*** impl of reactions to results data change */

  def intervalAdded(e: ListDataEvent) {
    updatePopup
  }

  def intervalRemoved(e: ListDataEvent) {
    updatePopup
  }

  def contentsChanged(e: ListDataEvent) {
    updatePopup
  }

  /**
   * Updates size and visibility of this panel according to model content
   */
  def updatePopup {
    val modelSize = ResultsModel.getSize

    // plug this popup into layered pane if needed
    val lPane = JLayeredPane.getLayeredPaneAbove(comboBar)
    if (lPane == null) {
      // #162075 - return when comboBar not yet seeded in AWT hierarchy
      return
    }
    if (!isDisplayable) {
      lPane.add(this, JLayeredPane.POPUP_LAYER.intValue + 1)
    }

    val statusVisible = updateStatusPanel

    computePopupBounds(popupBounds, lPane, modelSize)
    setBounds(popupBounds)

    // popup visibility constraints
    if ((modelSize > 0 || statusVisible) && comboBar.command.isFocusOwner) {
      if (modelSize > 0 && !isVisible) {
        jList1.setSelectedIndex(0)
      }
      if (jList1.getSelectedIndex >= modelSize) {
        jList1.setSelectedIndex(modelSize - 1)
      }
      setVisible(true)
    } else {
      setVisible(false)
    }

    // needed on JDK 1.5.x to repaint correctly
    revalidate
  }

  def getCategoryWidth: Int = {
    if (catWidth <= 0) {
      catWidth = computeWidth(jList1, 20, 30)
    }
    catWidth
  }

  def getResultWidth: Int = {
    if (resultWidth <= 0) {
      resultWidth = computeWidth(jList1, 42, 50)
    }
    resultWidth
  }

  /** Implementation of TaskListener, listen to when providers are finished
   * with their searching work
   */
  def taskFinished(task: Task) {
    evalTask = null
    // update UI in ED thread
    if (SwingUtilities.isEventDispatchThread) {
      run
    } else {
      SwingUtilities.invokeLater(this)
    }
  }

  /** Runnable implementation, updates popup */
  def run {
    updatePopup
  }

  private def computePopupBounds(result: Rectangle, lPane: JLayeredPane, modelSize: Int) {
    val cSize = comboBar.getSize()
    val width = getCategoryWidth + getResultWidth + 3
    var location = new Point(cSize.width - width - 1, comboBar.getBottomLineY - 1)
    if (SwingUtilities.getWindowAncestor(comboBar) != null) {
      location = SwingUtilities.convertPoint(comboBar, location, lPane)
    }
    result.setLocation(location)

    // hack to make jList.getpreferredSize work correctly
    // JList is listening on ResultsModel same as us and order of listeners
    // is undefined, so we have to force update of JList's layout data
    jList1.setFixedCellHeight(15)
    jList1.setFixedCellHeight(-1)
    // end of hack

    jList1.setVisibleRowCount(modelSize)
    val preferredSize = jList1.getPreferredSize

    preferredSize.width = width
    preferredSize.height += statusPanel.getPreferredSize.height + 3

    result.setSize(preferredSize)
  }

  /** Updates visibility and content of status labels.
   *
   * @return true when update panel should be visible (some its part is visible),
   * false otherwise
   */
  private def updateStatusPanel: Boolean = {
    var shouldBeVisible = false

    val isInProgress = evalTask != null
    searchingSep.setVisible(isInProgress)
    searchingLabel.setVisible(isInProgress)
    comboBar match {
      case x: QuickSearchComboBar =>
        if (isInProgress) {
          x.startProgressAnimation
        } else {
          x.stopProgressAnimation
        }
      case _ =>
    }
    shouldBeVisible = shouldBeVisible || isInProgress

    val searchedNotEmpty = searchedText != null && searchedText.trim.length > 0
    val areNoResults = ResultsModel.getSize <= 0 && searchedNotEmpty && !isInProgress
    noResultsLabel.setVisible(areNoResults)
    comboBar.setNoResults(areNoResults)
    shouldBeVisible = shouldBeVisible || areNoResults

    hintLabel.setText(getHintText)
    val isNarrowed = CommandEvaluator.evalCat != null && searchedNotEmpty
    hintSep.setVisible(isNarrowed)
    hintLabel.setVisible(isNarrowed)
    shouldBeVisible = shouldBeVisible || isNarrowed

    shouldBeVisible
  }

  private def getHintText: String = {
    val evalCat = CommandEvaluator.evalCat
    if (evalCat == null) {
      null
    } else NbBundle.getMessage(classOf[QuickSearchPopup], "QuickSearchPopup.hintLabel.text",
                               evalCat.displayName, SearchResultRender.getKeyStrokeAsText(comboBar.keyStroke))
  }

  /** Computes width of string up to maxCharCount, with font of given JComponent
   * and with maximum percentage of owning Window that can be taken */
  private def computeWidth (comp: JComponent, maxCharCount: Int, percent: Int): Int = {
    val fm = comp.getFontMetrics(comp.getFont)
    val charW = fm.charWidth('X')
    var result = charW * maxCharCount
    // limit width to 50% of containing window
    val w = SwingUtilities.windowForComponent(comp)
    if (w != null) {
      result = Math.min(result, w.getWidth * percent / 100)
    }
    result
  }

}
