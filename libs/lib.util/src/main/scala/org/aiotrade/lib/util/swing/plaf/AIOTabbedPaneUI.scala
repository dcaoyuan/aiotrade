package org.aiotrade.lib.util.swing.plaf

import javax.swing._
import java.awt._
import javax.swing.plaf._
import javax.swing.plaf.basic.BasicTabbedPaneUI
import javax.swing.plaf.metal.MetalLookAndFeel

/**
 * The Metal subclass of BasicTabbedPaneUI.
 * <p>
 * <strong>Warning:</strong>
 * Serialized objects of this class will not be compatible with
 * future Swing releases. The current serialization support is
 * appropriate for short term storage or RMI between applications running
 * the same version of Swing.  As of 1.4, support for long term storage
 * of all JavaBeans<sup><font size="-2">TM</font></sup>
 * has been added to the <code>java.beans</code> package.
 * Please see {@link java.beans.XMLEncoder}.
 *
 * @author Caoyuan Deng
 */
object AIOTabbedPaneUI {
  def createUI(x:JComponent) :ComponentUI = {
    new AIOTabbedPaneUI
  }
}
class AIOTabbedPaneUI extends BasicTabbedPaneUI {

  protected val minTabWidth = 40
  // Background color for unselected tabs that don't have an explicitly
  // set color.
  private var unselectedBackground: Color = _
  protected var tabAreaBackground: Color = _
  protected var selectColor: Color = _
  protected var selectHighlight: Color = _
  protected var selectedBorderColor: Color = _
  private var tabsOpaque: Boolean = true
  // Whether or not we're using ocean. This is cached as it is used
  // extensively during painting.
  private var ocean: Boolean = _
  // Selected border color for ocean.
  private var oceanSelectedBorderColor: Color = _

  override protected def createLayoutManager :LayoutManager = {
    if (tabPane.getTabLayoutPolicy == JTabbedPane.SCROLL_TAB_LAYOUT) {
      super.createLayoutManager
    } else {
      new MyTabbedPaneLayout
    }
  }

  override
  protected def installDefaults: Unit = {
    super.installDefaults

    tabAreaBackground = UIManager.getColor("TabbedPane.tabAreaBackground")
    selectColor = UIManager.getColor("TabbedPane.selected")
    selectHighlight = UIManager.getColor("TabbedPane.selectHighlight")
    tabsOpaque = UIManager.getBoolean("TabbedPane.tabsOpaque")
    unselectedBackground = UIManager.getColor("TabbedPane.unselectedBackground")
    selectedBorderColor = UIManager.getColor("TabbedPane.selectedBorderColor")
    ocean = false
    
    if (ocean) {
      oceanSelectedBorderColor = UIManager.getColor("TabbedPane.borderHightlightColor")
    }
  }

  override
  protected def paintTabBorder(g: Graphics, tabPlacement: Int,
                               tabIndex: Int, x: Int, y: Int, w: Int, h: Int,
                               isSelected: Boolean): Unit = {
    val bottom = y + (h - 1)
    val right  = x + (w - 1)

    tabPlacement match {
      case SwingConstants.LEFT =>
        paintLeftTabBorder(tabIndex, g, x, y, w, h, bottom, right, isSelected)
      case SwingConstants.BOTTOM =>
        paintBottomTabBorder(tabIndex, g, x, y, w, h, bottom, right, isSelected)
      case SwingConstants.RIGHT =>
        paintRightTabBorder(tabIndex, g, x, y, w, h, bottom, right, isSelected)
      case _ =>
        paintTopTabBorder(tabIndex, g, x, y, w, h, bottom, right, isSelected)
    }
  }

  protected def paintTopTabBorder(tabIndex: Int, g: Graphics,
                                  x: Int, y: Int, w: Int, h: Int,
                                  btm: Int, rght: Int,
                                  isSelected: Boolean): Unit = {

    val currentRun = getRunForTab(tabPane.getTabCount(), tabIndex)
    val lastIndex = lastTabInRun(tabPane.getTabCount(), currentRun)
    val firstIndex = tabRuns(currentRun)

    val leftToRight = isLeftToRight
    val selectedIndex = tabPane.getSelectedIndex
    val bottom = h - 1
    val right = w - 1

    //
    // Paint Gap
    //

    if (shouldFillGap(currentRun, tabIndex, x, y)) {
      g.translate(x, y)

      if (leftToRight) {
        g.setColor(getColorForGap(currentRun, x, y + 1))
        g.fillRect(1, 0, 5, 3)
        g.fillRect(1, 3, 2, 2)
      } else {
        g.setColor(getColorForGap(currentRun, x + w - 1, y + 1))
        g.fillRect(right - 5, 0, 5, 3)
        g.fillRect(right - 2, 3, 2, 2)
      }

      g.translate(-x, -y)
    }

    g.translate(x, y)

    //
    // Paint Border
    //

    if (ocean && isSelected) {
      g.setColor(oceanSelectedBorderColor)
    } else {
      g.setColor(darkShadow)
    }

    if (leftToRight) {

      // Paint slant
      g.drawLine(1, 5, 6, 0)

      // Paint top
      g.drawLine(6, 0, right, 0)

      // Paint right
      if (tabIndex == lastIndex) {
        // last tab in run
        g.drawLine(right, 1, right, bottom)
      }

      if (ocean && tabIndex - 1 == selectedIndex &&
          currentRun == getRunForTab(tabPane.getTabCount, selectedIndex)) {
        g.setColor(oceanSelectedBorderColor)
      }

      // Paint left
      if (tabIndex != tabRuns(runCount - 1)) {
        // not the first tab in the last run
        if (ocean && isSelected) {
          g.drawLine(0, 6, 0, bottom)
          g.setColor(darkShadow)
          g.drawLine(0, 0, 0, 5)
        } else {
          g.drawLine(0, 0, 0, bottom)
        }
      } else {
        // the first tab in the last run
        g.drawLine(0, 6, 0, bottom)
      }
    } else {

      // Paint slant
      g.drawLine(right - 1, 5, right - 6, 0)

      // Paint top
      g.drawLine(right - 6, 0, 0, 0)

      // Paint left
      if (tabIndex == lastIndex) {
        // last tab in run
        g.drawLine(0, 1, 0, bottom)
      }

      // Paint right
      if (ocean && tabIndex - 1 == selectedIndex &&
          currentRun == getRunForTab(tabPane.getTabCount, selectedIndex)) {
        g.setColor(oceanSelectedBorderColor)
        g.drawLine(right, 0, right, bottom)
      } else if (ocean && isSelected) {
        g.drawLine(right, 6, right, bottom)
        if (tabIndex != 0) {
          g.setColor(darkShadow)
          g.drawLine(right, 0, right, 5)
        }
      } else {
        if (tabIndex != tabRuns(runCount - 1)) {
          // not the first tab in the last run
          g.drawLine(right, 0, right, bottom)
        } else {
          // the first tab in the last run
          g.drawLine(right, 6, right, bottom)
        }
      }
    }

    //
    // Paint Highlight
    //

    g.setColor(if (isSelected) selectHighlight else highlight)

    if (leftToRight) {

      // Paint slant
      g.drawLine(1, 6, 6, 1)

      // Paint top
      g.drawLine(6, 1, if (tabIndex == lastIndex) right - 1 else right, 1)

      // Paint left
      g.drawLine(1, 6, 1, bottom)

      // paint highlight in the gap on tab behind this one
      // on the left end (where they all line up)
      if (tabIndex == firstIndex && tabIndex != tabRuns(runCount - 1)) {
        //  first tab in run but not first tab in last run
        if (tabPane.getSelectedIndex() == tabRuns(currentRun + 1)) {
          // tab in front of selected tab
          g.setColor(selectHighlight)
        } else {
          // tab in front of normal tab
          g.setColor(highlight)
        }
        g.drawLine(1, 0, 1, 4)
      }
    } else {

      // Paint slant
      g.drawLine(right - 1, 6, right - 6, 1)

      // Paint top
      g.drawLine(right - 6, 1, 1, 1)

      // Paint left
      if (tabIndex == lastIndex) {
        // last tab in run
        g.drawLine(1, 1, 1, bottom)
      } else {
        g.drawLine(0, 1, 0, bottom)
      }
    }

    g.translate(-x, -y)
  }

  protected def shouldFillGap(currentRun: Int, tabIndex: Int, x: Int, y: Int): Boolean = {
    var result = false

    if (!tabsOpaque) {
      return false
    }

    if (currentRun == runCount - 2) {  // If it's the second to last row.
      val lastTabBounds = getTabBounds(tabPane, tabPane.getTabCount - 1)
      val tabBounds = getTabBounds(tabPane, tabIndex)
      if (isLeftToRight) {
        val lastTabRight = lastTabBounds.x + lastTabBounds.width - 1

        // is the right edge of the last tab to the right
        // of the left edge of the current tab?
        if (lastTabRight > tabBounds.x + 2) {
          return true
        }
      } else {
        val lastTabLeft = lastTabBounds.x
        val currentTabRight = tabBounds.x + tabBounds.width - 1

        // is the left edge of the last tab to the left
        // of the right edge of the current tab?
        if (lastTabLeft < currentTabRight - 2) {
          return true
        }
      }
    } else {
      // fill in gap for all other rows except last row
      result = currentRun != runCount - 1
    }

    return result
  }

  protected def getColorForGap(currentRun: Int, x: Int, y: Int): Color = {
    val shadowWidth = 4
    val selectedIndex = tabPane.getSelectedIndex
    val startIndex = tabRuns(currentRun + 1)
    val endIndex = lastTabInRun(tabPane.getTabCount(), currentRun + 1)
    val tabOverGap = -1
    // Check each tab in the row that is 'on top' of this row
    for (i <- startIndex to endIndex) {
      val tabBounds = getTabBounds(tabPane, i)
      val tabLeft = tabBounds.x
      val tabRight = (tabBounds.x + tabBounds.width) - 1
      // Check to see if this tab is over the gap
      if (isLeftToRight) {
        if (tabLeft <= x && tabRight - shadowWidth > x) {
          return if (selectedIndex == i) selectColor else getUnselectedBackgroundAt(i)
        }
      } else {
        if (tabLeft + shadowWidth < x && tabRight >= x) {
          return if (selectedIndex == i) selectColor else getUnselectedBackgroundAt(i)
        }
      }
    }

    tabPane.getBackground
  }

  protected def paintLeftTabBorder(tabIndex: Int, g: Graphics,
                                   x: Int, y: Int, w: Int, h: Int,
                                   btm: Int, rght: Int,
                                   isSelected: Boolean): Unit = {
    val tabCount = tabPane.getTabCount
    val currentRun = getRunForTab(tabCount, tabIndex)
    val lastIndex = lastTabInRun(tabCount, currentRun)
    val firstIndex = tabRuns(currentRun)

    g.translate(x, y)

    val bottom = h - 1
    val right = w - 1

    //
    // Paint part of the tab above
    //

    if (tabIndex != firstIndex && tabsOpaque) {
      g.setColor(if (tabPane.getSelectedIndex == tabIndex - 1) selectColor else getUnselectedBackgroundAt(tabIndex - 1))
      g.fillRect(2, 0, 4, 3)
      g.drawLine(2, 3, 2, 3)
    }


    //
    // Paint Highlight
    //

    if (ocean) {
      g.setColor(if (isSelected) selectHighlight else MetalLookAndFeel.getWhite)
    } else {
      g.setColor(if (isSelected) selectHighlight else highlight)
    }

    // Paint slant
    g.drawLine(1, 6, 6, 1)

    // Paint left
    g.drawLine(1, 6, 1, bottom)

    // Paint top
    g.drawLine(6, 1, right, 1)

    if (tabIndex != firstIndex) {
      if (tabPane.getSelectedIndex == tabIndex - 1) {
        g.setColor(selectHighlight)
      } else {
        g.setColor(if (ocean) MetalLookAndFeel.getWhite else highlight)
      }

      g.drawLine(1, 0, 1, 4)
    }

    //
    // Paint Border
    //

    if (ocean) {
      if (isSelected) {
        g.setColor(oceanSelectedBorderColor)
      } else {
        g.setColor(darkShadow)
      }
    } else {
      g.setColor(darkShadow)
    }

    // Paint slant
    g.drawLine(1, 5, 6, 0)

    // Paint top
    g.drawLine(6, 0, right, 0)

    // Paint bottom
    if (tabIndex == lastIndex) {
      g.drawLine(0, bottom, right, bottom)
    }

    // Paint left
    if (ocean) {
      if (tabPane.getSelectedIndex == tabIndex - 1) {
        g.drawLine(0, 5, 0, bottom)
        g.setColor(oceanSelectedBorderColor)
        g.drawLine(0, 0, 0, 5)
      } else if (isSelected) {
        g.drawLine(0, 6, 0, bottom)
        if (tabIndex != 0) {
          g.setColor(darkShadow)
          g.drawLine(0, 0, 0, 5)
        }
      } else if (tabIndex != firstIndex) {
        g.drawLine(0, 0, 0, bottom)
      } else {
        g.drawLine(0, 6, 0, bottom)
      }
    } else { // metal
      if (tabIndex != firstIndex) {
        g.drawLine(0, 0, 0, bottom)
      } else {
        g.drawLine(0, 6, 0, bottom)
      }
    }

    g.translate(-x, -y)
  }

  protected def paintBottomTabBorder(tabIndex: Int, g: Graphics,
                                     x: Int, y: Int, w: Int, h: Int,
                                     btm: Int, rght: Int,
                                     isSelected: Boolean): Unit = {
    val tabCount = tabPane.getTabCount
    val currentRun = getRunForTab(tabCount, tabIndex)
    val lastIndex = lastTabInRun(tabCount, currentRun)
    val firstIndex = tabRuns(currentRun)
    val leftToRight = isLeftToRight

    val bottom = h - 1
    val right = w - 1

    //
    // Paint Gap
    //

    if (shouldFillGap(currentRun, tabIndex, x, y)) {
      g.translate(x, y)

      if (leftToRight) {
        g.setColor(getColorForGap(currentRun, x, y))
        g.fillRect(1, bottom - 4, 3, 5)
        g.fillRect(4, bottom - 1, 2, 2)
      } else {
        g.setColor(getColorForGap(currentRun, x + w - 1, y))
        g.fillRect(right - 3, bottom - 3, 3, 4)
        g.fillRect(right - 5, bottom - 1, 2, 2)
        g.drawLine(right - 1, bottom - 4, right - 1, bottom - 4)
      }

      g.translate(-x, -y)
    }

    g.translate(x, y)


    //
    // Paint Border
    //

    if (ocean && isSelected) {
      g.setColor(oceanSelectedBorderColor)
    } else {
      g.setColor(darkShadow)
    }

    if (leftToRight) {

      // Paint slant
      g.drawLine(1, bottom - 5, 6, bottom)

      // Paint bottom
      g.drawLine(6, bottom, right, bottom)

      // Paint right
      if (tabIndex == lastIndex) {
        g.drawLine(right, 0, right, bottom)
      }

      // Paint left
      if (ocean && isSelected) {
        g.drawLine(0, 0, 0, bottom - 6)
        if ((currentRun == 0 && tabIndex != 0) ||
            (currentRun > 0 && tabIndex != tabRuns(currentRun - 1))) {
          g.setColor(darkShadow)
          g.drawLine(0, bottom - 5, 0, bottom)
        }
      } else {
        if (ocean && tabIndex == tabPane.getSelectedIndex + 1) {
          g.setColor(oceanSelectedBorderColor)
        }
        if (tabIndex != tabRuns(runCount - 1)) {
          g.drawLine(0, 0, 0, bottom)
        } else {
          g.drawLine(0, 0, 0, bottom - 6)
        }
      }
    } else {

      // Paint slant
      g.drawLine(right - 1, bottom - 5, right - 6, bottom)

      // Paint bottom
      g.drawLine(right - 6, bottom, 0, bottom)

      // Paint left
      if (tabIndex == lastIndex) {
        // last tab in run
        g.drawLine(0, 0, 0, bottom)
      }

      // Paint right
      if (ocean && tabIndex == tabPane.getSelectedIndex + 1) {
        g.setColor(oceanSelectedBorderColor)
        g.drawLine(right, 0, right, bottom)
      } else if (ocean && isSelected) {
        g.drawLine(right, 0, right, bottom - 6)
        if (tabIndex != firstIndex) {
          g.setColor(darkShadow)
          g.drawLine(right, bottom - 5, right, bottom)
        }
      } else if (tabIndex != tabRuns(runCount - 1)) {
        // not the first tab in the last run
        g.drawLine(right, 0, right, bottom)
      } else {
        // the first tab in the last run
        g.drawLine(right, 0, right, bottom - 6)
      }
    }

    //
    // Paint Highlight
    //

    g.setColor(if (isSelected) selectHighlight else highlight)

    if (leftToRight) {

      // Paint slant
      g.drawLine(1, bottom - 6, 6, bottom - 1)

      // Paint left
      g.drawLine(1, 0, 1, bottom - 6)

      // paint highlight in the gap on tab behind this one
      // on the left end (where they all line up)
      if (tabIndex == firstIndex && tabIndex != tabRuns(runCount - 1)) {
        //  first tab in run but not first tab in last run
        if (tabPane.getSelectedIndex == tabRuns(currentRun + 1)) {
          // tab in front of selected tab
          g.setColor(selectHighlight)
        } else {
          // tab in front of normal tab
          g.setColor(highlight)
        }
        g.drawLine(1, bottom - 4, 1, bottom)
      }
    } else {

      // Paint left
      if (tabIndex == lastIndex) {
        // last tab in run
        g.drawLine(1, 0, 1, bottom - 1)
      } else {
        g.drawLine(0, 0, 0, bottom - 1)
      }
    }

    g.translate(-x, -y)
  }

  protected def paintRightTabBorder(tabIndex: Int, g: Graphics,
                                    x: Int, y: Int, w: Int, h: Int,
                                    btm: Int, rght: Int,
                                    isSelected: Boolean): Unit = {
    val tabCount = tabPane.getTabCount
    val currentRun = getRunForTab(tabCount, tabIndex)
    val lastIndex = lastTabInRun(tabCount, currentRun)
    val firstIndex = tabRuns(currentRun)

    g.translate(x, y)

    val bottom = h - 1
    val right  = w - 1

    //
    // Paint part of the tab above
    //

    if (tabIndex != firstIndex && tabsOpaque) {
      g.setColor(if (tabPane.getSelectedIndex == tabIndex - 1) selectColor else getUnselectedBackgroundAt(tabIndex - 1))
      g.fillRect(right - 5, 0, 5, 3)
      g.fillRect(right - 2, 3, 2, 2)
    }


    //
    // Paint Highlight
    //

    g.setColor(if (isSelected) selectHighlight else highlight)

    // Paint slant
    g.drawLine(right - 6, 1, right - 1, 6)

    // Paint top
    g.drawLine(0, 1, right - 6, 1)

    // Paint left
    if (!isSelected) {
      g.drawLine(0, 1, 0, bottom)
    }


    //
    // Paint Border
    //

    if (ocean && isSelected) {
      g.setColor(oceanSelectedBorderColor)
    } else {
      g.setColor(darkShadow)
    }

    // Paint bottom
    if (tabIndex == lastIndex) {
      g.drawLine(0, bottom, right, bottom)
    }

    // Paint slant
    if (ocean && tabPane.getSelectedIndex == tabIndex - 1) {
      g.setColor(oceanSelectedBorderColor)
    }
    g.drawLine(right - 6, 0, right, 6)

    // Paint top
    g.drawLine(0, 0, right - 6, 0)

    // Paint right
    if (ocean && isSelected) {
      g.drawLine(right, 6, right, bottom)
      if (tabIndex != firstIndex) {
        g.setColor(darkShadow)
        g.drawLine(right, 0, right, 5)
      }
    } else if (ocean && tabPane.getSelectedIndex == tabIndex - 1) {
      g.setColor(oceanSelectedBorderColor)
      g.drawLine(right, 0, right, 6)
      g.setColor(darkShadow)
      g.drawLine(right, 6, right, bottom)
    } else if (tabIndex != firstIndex) {
      g.drawLine(right, 0, right, bottom)
    } else {
      g.drawLine(right, 6, right, bottom)
    }

    g.translate(-x, -y)
  }

  override def update(g: Graphics, c: JComponent): Unit = {
    if (c.isOpaque) {
      g.setColor(tabAreaBackground)
      g.fillRect(0, 0, c.getWidth, c.getHeight)
    }
    paint(g, c)
  }

  override protected def paintTabBackground(g: Graphics, tabPlacement: Int,
                                            tabIndex: Int, x: Int, y: Int, w: Int, h: Int, isSelected: Boolean): Unit = {
    val slantWidth = h / 2
    if (isSelected) {
      g.setColor(selectColor)
    } else {
      g.setColor(getUnselectedBackgroundAt(tabIndex))
    }

    if (isLeftToRight) {
      tabPlacement match {
        case SwingConstants.LEFT =>
          g.fillRect(x + 5, y + 1, w - 5, h - 1)
          g.fillRect(x + 2, y + 4, 3, h - 4)
        case SwingConstants.BOTTOM =>
          g.fillRect(x + 2, y, w - 2, h - 4)
          g.fillRect(x + 5, y + (h - 1) - 3, w - 5, 3)
        case SwingConstants.RIGHT =>
          g.fillRect(x, y + 2, w - 4, h - 2)
          g.fillRect(x + (w - 1) - 3, y + 5, 3, h - 5)
        case SwingConstants.TOP | _ =>
          g.fillRect(x + 4, y + 2, (w - 1) - 3, (h - 1) - 1)
          g.fillRect(x + 2, y + 5, 2, h - 5)
      }
    } else {
      tabPlacement match {
        case SwingConstants.LEFT =>
          g.fillRect(x + 5, y + 1, w - 5, h - 1)
          g.fillRect(x + 2, y + 4, 3, h - 4)
        case SwingConstants.BOTTOM =>
          g.fillRect(x, y, w - 5, h - 1)
          g.fillRect(x + (w - 1) - 4, y, 4, h - 5)
          g.fillRect(x + (w - 1) - 4, y + (h - 1) - 4, 2, 2)
        case SwingConstants.RIGHT =>
          g.fillRect(x + 1, y + 1, w - 5, h - 1)
          g.fillRect(x + (w - 1) - 3, y + 5, 3, h - 5)
        case SwingConstants.TOP | _ =>
          g.fillRect(x, y + 2, (w - 1) - 3, (h - 1) - 1)
          g.fillRect(x + (w - 1) - 3, y + 5, 3, h - 3)
      }
    }
  }

  /**
   * Overridden to do nothing for the Java L&F.
   */
  override protected def getTabLabelShiftX(tabPlacement: Int, tabIndex: Int, isSelected: Boolean): Int = {
    0
  }

  /**
   * Overridden to do nothing for the Java L&F.
   */
  override protected def getTabLabelShiftY(tabPlacement: Int, tabIndex: Int, isSelected: Boolean): Int = {
    0
  }

  /**
   * {@inheritDoc}
   *
   * @since 1.6
   */
  override protected def getBaselineOffset: Int = {
    0
  }

  override def paint(g: Graphics, c: JComponent): Unit = {
    val tabPlacement = tabPane.getTabPlacement

    val insets = c.getInsets
    val size = c.getSize()

    // Paint the background for the tab area
    if (tabPane.isOpaque) {
      val bg = UIManager.getColor("TabbedPane.tabAreaBackground")
      if (bg != null) {
        g.setColor(bg)
      } else {
        g.setColor(c.getBackground)
      }
      tabPlacement match {
        case SwingConstants.LEFT =>
          g.fillRect(insets.left, insets.top,
                     calculateTabAreaWidth(tabPlacement, runCount, maxTabWidth),
                     size.height - insets.bottom - insets.top)
        case SwingConstants.BOTTOM =>
          val totalTabHeight = calculateTabAreaHeight(tabPlacement, runCount, maxTabHeight)
          g.fillRect(insets.left, size.height - insets.bottom - totalTabHeight,
                     size.width - insets.left - insets.right,
                     totalTabHeight)
        case SwingConstants.RIGHT =>
          val totalTabWidth = calculateTabAreaWidth(tabPlacement, runCount, maxTabWidth)
          g.fillRect(size.width - insets.right - totalTabWidth,
                     insets.top, totalTabWidth,
                     size.height - insets.top - insets.bottom)
        case SwingConstants.TOP | _ =>
          g.fillRect(insets.left, insets.top,
                     size.width - insets.right - insets.left,
                     calculateTabAreaHeight(tabPlacement, runCount, maxTabHeight))
          paintHighlightBelowTab
      }
    }

    super.paint(g, c)
  }

  protected def paintHighlightBelowTab: Unit = {
  }

  override
  protected def paintFocusIndicator(g: Graphics, tabPlacement: Int,
                                    rects: Array[Rectangle], tabIndex: Int,
                                    iconRect: Rectangle, textRect: Rectangle,
                                    isSelected: Boolean): Unit = {

    if (tabPane.hasFocus() && isSelected) {
      val tabRect = rects(tabIndex)
      val lastInRun = isLastInRun(tabIndex)
      g.setColor(focus)
      g.translate(tabRect.x, tabRect.y)
      val right = tabRect.width - 1
      val bottom = tabRect.height - 1
      val leftToRight = isLeftToRight
      tabPlacement match {
        case SwingConstants.RIGHT =>
          g.drawLine(right - 6, 2, right - 2, 6)         // slant
          g.drawLine(1, 2, right - 6, 2)                 // top
          g.drawLine(right - 2, 6, right - 2, bottom)    // right
          g.drawLine(1, 2, 1, bottom)                    // left
          g.drawLine(1, bottom, right - 2, bottom)       // bottom
        case SwingConstants.BOTTOM =>
          if (leftToRight) {
            g.drawLine(2, bottom - 6, 6, bottom - 2)   // slant
            g.drawLine(6, bottom - 2,
                       right, bottom - 2)              // bottom
            g.drawLine(2, 0, 2, bottom - 6)            // left
            g.drawLine(2, 0, right, 0)                 // top
            g.drawLine(right, 0, right, bottom - 2)    // right
          } else {
            g.drawLine(right - 2, bottom - 6,
                       right - 6, bottom - 2)          // slant
            g.drawLine(right - 2, 0,
                       right - 2, bottom - 6)          // right
            if (lastInRun) {
              // last tab in run
              g.drawLine(2, bottom - 2,
                         right - 6, bottom - 2)      // bottom
              g.drawLine(2, 0, right - 2, 0)         // top
              g.drawLine(2, 0, 2, bottom - 2)        // left
            } else {
              g.drawLine(1, bottom - 2,
                         right - 6, bottom - 2)      // bottom
              g.drawLine(1, 0, right - 2, 0)         // top
              g.drawLine(1, 0, 1, bottom - 2)        // left
            }
          }
        case SwingConstants.LEFT =>
          g.drawLine(2, 6, 6, 2)                         // slant
          g.drawLine(2, 6, 2, bottom - 1)                 // left
          g.drawLine(6, 2, right, 2)                     // top
          g.drawLine(right, 2, right, bottom - 1)        // right
          g.drawLine(2, bottom - 1,
                     right, bottom - 1)                  // bottom
        case SwingConstants.TOP | _ =>
          if (leftToRight) {
            g.drawLine(2, 6, 6, 2)                     // slant
            g.drawLine(2, 6, 2, bottom - 1)             // left
            g.drawLine(6, 2, right, 2)                 // top
            g.drawLine(right, 2, right, bottom - 1)    // right
            g.drawLine(2, bottom - 1,
                       right, bottom - 1)              // bottom
          } else {
            g.drawLine(right - 2, 6, right - 6, 2)     // slant
            g.drawLine(right - 2, 6,
                       right - 2, bottom - 1)           // right
            if (lastInRun) {
              // last tab in run
              g.drawLine(right - 6, 2, 2, 2)         // top
              g.drawLine(2, 2, 2, bottom - 1)        // left
              g.drawLine(right - 2, bottom - 1,
                         2, bottom - 1)              // bottom
            } else {
              g.drawLine(right - 6, 2, 1, 2)         // top
              g.drawLine(1, 2, 1, bottom - 1)        // left
              g.drawLine(right - 2, bottom - 1,
                         1, bottom - 1)              // bottom
            }
          }
      }
      g.translate(-tabRect.x, -tabRect.y)
    }
  }

  override protected def paintContentBorderTopEdge(g: Graphics, tabPlacement: Int,
                                                   selectedIndex: Int,
                                                   x: Int, y: Int, w: Int, h: Int): Unit = {
    val leftToRight = isLeftToRight
    val right = x + w - 1
    val selRect = if (selectedIndex < 0) null else getTabBounds(selectedIndex, calcRect)
    if (ocean) {
      g.setColor(oceanSelectedBorderColor)
    } else {
      if (selectedBorderColor != null) {
        g.setColor(selectedBorderColor)
      } else {
        g.setColor(selectHighlight)
      }
    }

    // Draw unbroken line if tabs are not on TOP, OR
    // selected tab is not in run adjacent to content, OR
    // selected tab is not visible (SCROLL_TAB_LAYOUT)
    //
    if (tabPlacement != SwingConstants.TOP || selectedIndex < 0 ||
        (selRect.y + selRect.height + 1 < y) ||
        (selRect.x < x || selRect.x > x + w)) {
      g.drawLine(x, y, x + w - 2, y)
      if (ocean && tabPlacement == SwingConstants.TOP) {
        g.setColor(MetalLookAndFeel.getWhite())
        g.drawLine(x, y + 1, x + w - 2, y + 1)
      }
    } else {
      // Break line to show visual connection to selected tab
      val lastInRun = isLastInRun(selectedIndex)

      if (leftToRight || lastInRun) {
        g.drawLine(x, y, selRect.x + 1, y)
      } else {
        g.drawLine(x, y, selRect.x, y)
      }

      if (selRect.x + selRect.width < right - 1) {
        if (leftToRight && !lastInRun) {
          g.drawLine(selRect.x + selRect.width, y, right - 1, y)
        } else {
          g.drawLine(selRect.x + selRect.width - 1, y, right - 1, y)
        }
      } else {
        g.setColor(shadow)
        g.drawLine(x + w - 2, y, x + w - 2, y)
      }

      if (ocean) {
        g.setColor(MetalLookAndFeel.getWhite)

        if (leftToRight || lastInRun) {
          g.drawLine(x, y + 1, selRect.x + 1, y + 1)
        } else {
          g.drawLine(x, y + 1, selRect.x, y + 1)
        }

        if (selRect.x + selRect.width < right - 1) {
          if (leftToRight && !lastInRun) {
            g.drawLine(selRect.x + selRect.width, y + 1, right - 1, y + 1)
          } else {
            g.drawLine(selRect.x + selRect.width - 1, y + 1, right - 1, y + 1)
          }
        } else {
          g.setColor(shadow)
          g.drawLine(x + w - 2, y + 1, x + w - 2, y + 1)
        }
      }
    }
  }

  override protected def paintContentBorderBottomEdge(g: Graphics, tabPlacement: Int,
                                                      selectedIndex: Int,
                                                      x: Int, y: Int, w: Int, h: Int): Unit = {
    val leftToRight = isLeftToRight
    val bottom = y + h - 1
    val right = x + w - 1
    val selRect = if (selectedIndex < 0) null else getTabBounds(selectedIndex, calcRect)

    g.setColor(darkShadow)

    // Draw unbroken line if tabs are not on BOTTOM, OR
    // selected tab is not in run adjacent to content, OR
    // selected tab is not visible (SCROLL_TAB_LAYOUT)
    //
    if (tabPlacement != SwingConstants.BOTTOM || selectedIndex < 0 ||
        (selRect.y - 1 > h) ||
        (selRect.x < x || selRect.x > x + w)) {
      if (ocean && tabPlacement == SwingConstants.BOTTOM) {
        g.setColor(oceanSelectedBorderColor)
      }
      g.drawLine(x + 2, y + h - 2, x + w - 1, y + h - 2)
    } else {
      // Break line to show visual connection to selected tab
      val lastInRun = isLastInRun(selectedIndex)

      if (ocean) {
        g.setColor(oceanSelectedBorderColor)
      }

      if (leftToRight || lastInRun) {
        g.drawLine(x, bottom, selRect.x, bottom)
      } else {
        g.drawLine(x, bottom, selRect.x - 1, bottom)
      }

      if (selRect.x + selRect.width < x + w - 2) {
        if (leftToRight && !lastInRun) {
          g.drawLine(selRect.x + selRect.width, bottom, right, bottom)
        } else {
          g.drawLine(selRect.x + selRect.width - 1, bottom, right, bottom)
        }
      }
    }
  }

  override protected def paintContentBorderLeftEdge(g: Graphics, tabPlacement: Int,
                                                    selectedIndex: Int,
                                                    x: Int, y: Int, w: Int, h: Int): Unit = {
    val selRect = if (selectedIndex < 0) null else getTabBounds(selectedIndex, calcRect)
    if (ocean) {
      g.setColor(oceanSelectedBorderColor)
    } else {
      if (selectedBorderColor != null) {
        g.setColor(selectedBorderColor)
      } else {
        g.setColor(selectHighlight)
      }
    }

    // Draw unbroken line if tabs are not on LEFT, OR
    // selected tab is not in run adjacent to content, OR
    // selected tab is not visible (SCROLL_TAB_LAYOUT)
    //
    if (tabPlacement != SwingConstants.LEFT || selectedIndex < 0 ||
        (selRect.x + selRect.width + 1 < x) ||
        (selRect.y < y || selRect.y > y + h)) {
      g.drawLine(x, y + 1, x, y + h - 2)
      if (ocean && tabPlacement == SwingConstants.LEFT) {
        g.setColor(MetalLookAndFeel.getWhite)
        g.drawLine(x + 1, y, x + 1, y + h - 2)
      }
    } else {
      // Break line to show visual connection to selected tab
      g.drawLine(x, y, x, selRect.y + 1)
      if (selRect.y + selRect.height < y + h - 2) {
        g.drawLine(x, selRect.y + selRect.height + 1,
                   x, y + h + 2)
      }
      if (ocean) {
        g.setColor(MetalLookAndFeel.getWhite)
        g.drawLine(x + 1, y + 1, x + 1, selRect.y + 1)
        if (selRect.y + selRect.height < y + h - 2) {
          g.drawLine(x + 1, selRect.y + selRect.height + 1,
                     x + 1, y + h + 2)
        }
      }
    }
  }

  override protected def paintContentBorderRightEdge(g: Graphics, tabPlacement: Int,
                                                     selectedIndex: Int,
                                                     x: Int, y: Int, w: Int, h: Int): Unit = {
    val selRect = if (selectedIndex < 0) null else getTabBounds(selectedIndex, calcRect)

    g.setColor(darkShadow)
    // Draw unbroken line if tabs are not on RIGHT, OR
    // selected tab is not in run adjacent to content, OR
    // selected tab is not visible (SCROLL_TAB_LAYOUT)
    //
    if (tabPlacement != SwingConstants.RIGHT || selectedIndex < 0 ||
        (selRect.x - 1 > w) ||
        (selRect.y < y || selRect.y > y + h)) {
      if (ocean && tabPlacement == SwingConstants.RIGHT) {
        g.setColor(oceanSelectedBorderColor)
      }
      g.drawLine(x + w - 1, y, x + w - 1, y + h - 1)
    } else {
      // Break line to show visual connection to selected tab
      if (ocean) {
        g.setColor(oceanSelectedBorderColor)
      }
      g.drawLine(x + w - 1, y, x + w - 1, selRect.y)

      if (selRect.y + selRect.height < y + h - 2) {
        g.drawLine(x + w - 1, selRect.y + selRect.height,
                   x + w - 1, y + h - 2)
      }
    }
  }

  override protected def calculateMaxTabHeight(tabPlacement: Int): Int = {
    val metrics = getFontMetrics
    val height = metrics.getHeight
    val tallerIcons = (0 until tabPane.getTabCount).find{i =>
      val icon = tabPane.getIconAt(i)
      icon != null && icon.getIconHeight > height
    }
    
    super.calculateMaxTabHeight(tabPlacement) - {if (tallerIcons != None) tabInsets.top + tabInsets.bottom else 0}
  }

  override protected def getTabRunOverlay(tabPlacement: Int): Int = {
    // Tab runs laid out vertically should overlap
    // at least as much as the largest slant
    if (tabPlacement == SwingConstants.LEFT || tabPlacement == SwingConstants.RIGHT) {
      val maxTabHeight1 = calculateMaxTabHeight(tabPlacement)
      maxTabHeight1 / 2
    } else 0
  }

  // Don't rotate runs!
  protected def shouldRotateTabRuns(tabPlacement: Int, selectedRun: Int): Boolean = {
    false
  }

  // Don't pad last run
  override protected def shouldPadTabRun(tabPlacement: Int, run: Int): Boolean = {
    runCount > 1 && run < runCount - 1
  }

  private def isLastInRun(tabIndex: Int): Boolean = {
    val run = getRunForTab(tabPane.getTabCount, tabIndex)
    val lastIndex = lastTabInRun(tabPane.getTabCount, run)
    tabIndex == lastIndex
  }

  /**
   * Returns the color to use for the specified tab.
   */
  private def getUnselectedBackgroundAt(index: Int): Color = {
    val color = tabPane.getBackgroundAt(index)
    if (color.isInstanceOf[UIResource]) {
      if (unselectedBackground != null) {
        return unselectedBackground
      }
    }
    color
  }

  /**
   * Returns the tab index of JTabbedPane the mouse is currently over
   */
  protected def getRolloverTabIndex: Int = {
    getRolloverTab
  }

  private def isLeftToRight: Boolean = {
    tabPane.getTabPlacement match {
      case SwingConstants.TOP | SwingConstants.BOTTOM => true
      case _ => false
    }
  }

  /**
   * This inner class is marked &quotpublic&quot due to a compiler bug.
   * This class should be treated as a &quotprotected&quot inner class.
   * Instantiate it only within subclasses of MetalTabbedPaneUI.
   */
  import BasicTabbedPaneUI._
  class MyTabbedPaneLayout extends TabbedPaneLayout {

    //AIOTabbedPaneUI.this.super()

    override protected def normalizeTabRuns(tabPlacement: Int, tabCount: Int,
                                            start: Int, max: Int): Unit = {
      // Only normalize the runs for top & bottom  normalizing
      // doesn't look right for Metal's vertical tabs
      // because the last run isn't padded and it looks odd to have
      // fat tabs in the first vertical runs, but slimmer ones in the
      // last (this effect isn't noticeable for horizontal tabs).
      if (tabPlacement == SwingConstants.TOP || tabPlacement == SwingConstants.BOTTOM) {
        super.normalizeTabRuns(tabPlacement, tabCount, start, max)
      }
    }

    // Don't rotate runs!
    override protected def rotateTabRuns(tabPlacement: Int, selectedRun: Int): Unit = {
    }

    // Don't pad selected tab
    override protected def padSelectedTab(tabPlacement: Int, selectedIndex: Int): Unit = {
    }
  }
}
