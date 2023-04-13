/*
 * (swing1.1beta3)
 * 
 */
package org.aiotrade.lib.util.swing.table

import java.awt.Color
import java.awt.Font
import javax.swing.SwingConstants

/**
 * @version 1.0 11/22/98
 */
abstract class AttrType
object AttrType {
  case object Foreground extends AttrType
  case object Background extends AttrType
  case object Font extends AttrType
  case object HorizontalAlignment extends AttrType
  case object VerticalAlignment extends AttrType
}

class DefaultCellAttribute(nRows: Int, nCols: Int) extends CellAttribute with CellSpan with ColoredCell with CellFont {

  protected class Attr {
    var font: Font = _
    var foreground: Color = _
    var background: Color = _
    var horizontalAlignment: Int = _
    var verticalAlignment: Int = _
  }

  //
  // !!!! CAUTION !!!!!
  // these values must be synchronized to Table data
  //
  protected var spans: Array[Array[Array[Int]]] = _  // CellSpan
  protected var attrs: Array[Array[Attr]] = _

  dim = Dim(nRows, nCols)

  def this() = {
    this(1, 1)
  }

  protected def rowSize: Int = spans.length
  protected def colSize: Int = if (spans.length > 0) spans(0).length else 0

  protected def initValue {
    for (i <- 0 until spans.length) {
      for (j <- 0 until spans(i).length) {
        spans(i)(j)(CellSpan.ROW) = 1
        spans(i)(j)(CellSpan.COL) = 1
      }
    }
  }

  def addColumn {
    val oldSpan = spans
    val nRows = oldSpan.length
    val nCols = oldSpan(0).length
    spans = Array.ofDim(nRows, nCols + 1, 2)
    System.arraycopy(oldSpan, 0, spans, 0, nRows)
    for (row <- 0 until nRows) {
      spans(row)(nCols)(CellSpan.COL) = 1
      spans(row)(nCols)(CellSpan.ROW) = 1
    }

    val oldAttr = attrs
    attrs = Array.ofDim(nRows, nCols + 1)
    System.arraycopy(oldAttr, 0, attrs, 0, nRows)
  }

  def addRow {
    val oldSpan = spans
    val nRows = oldSpan.length
    val nCols = oldSpan(0).length

    spans = Array.ofDim(nRows + 1, nCols, 2)
    System.arraycopy(oldSpan, 0, spans, 0, nRows)
    for (col <- 0 until nCols) {
      spans(nRows)(col)(CellSpan.COL) = 1
      spans(nRows)(col)(CellSpan.ROW) = 1
    }

    val oldAttr = attrs
    attrs = Array.ofDim(nRows, nCols + 1)
    System.arraycopy(oldAttr, 0, attrs, 0, nRows)
  }

  def insertRow(row: Int) {
    val oldSpan = spans
    val nRows = oldSpan.length
    val nCols = oldSpan(0).length
    spans = Array.ofDim(nRows + 1, nCols, 2)
    if (0 < row) {
      System.arraycopy(oldSpan, 0, spans, 0, row - 1)
    }
    System.arraycopy(oldSpan, 0, spans, row, nRows - row)
    for (col <- 0 until nCols) {
      spans(row)(col)(CellSpan.COL) = 1
      spans(row)(col)(CellSpan.ROW) = 1
    }
  }

  def dim: Dim = {
    Dim(rowSize, colSize)
  }

  def dim_=(dim: Dim) {
    spans = Array.ofDim(dim.nRows, dim.nCols, 2) // 2: ROW, COL
    attrs = Array.ofDim(dim.nRows, dim.nCols)
    initValue
  }

  /*
   public void changeAttribute(int row, int column, Object command) {
   }

   public void changeAttribute(int[] rows, int[] columns, Object command) {
   }
   */
  protected def isOutOfBounds(row: Int, col: Int): Boolean = {
    row < 0 || row >= rowSize || col < 0 || col >= colSize
  }

  protected def isOutOfBounds(rows: Array[Int], cols: Array[Int]): Boolean = {
    for (i <- 0 until rows.length) {
      if (rows(i) < 0 || rows(i) >= rowSize) {
        return true
      }
    }
    for (i <- 0 until cols.length) {
      if (cols(i) < 0 || cols(i) >= colSize) {
        return true
      }
    }

    false
  }

  // ----- CellSpan
  
  def getSpan(row: Int, column: Int): Array[Int] = {
    if (isOutOfBounds(row, column)) {
      Array(1, 1)
    } else {
      spans(row)(column)
    }
  }

  def setSpan(span: Array[Int], row: Int, col: Int) {
    if (isOutOfBounds(row, col)) {
      return
    }
    spans(row)(col) = span
  }

  def isVisible(row: Int, col: Int): Boolean = {
    if (isOutOfBounds(row, col)) {
      return false
    }
    spans(row)(col)(CellSpan.COL) > 0 && spans(row)(col)(CellSpan.ROW) > 0
  }

  def combine(rows: Array[Int], cols: Array[Int]) {
    if (isOutOfBounds(rows, cols)) {
      return
    }
    val rowSpan = rows.length
    val colSpan = cols.length
    val startRow = rows(0)
    val startCol = cols(0)
    
    for (i <- 0 until rowSpan) {
      for (j <- 0 until colSpan) {
        if (spans(startRow + i)(startCol + j)(CellSpan.COL) != 1 ||
            spans(startRow + i)(startCol + j)(CellSpan.ROW) != 1) {
          //System.out.println("can't combine");
          return
        }
      }
    }
    
    var ii = 0
    for (i <- 0 until rowSpan) {
      var jj = 0
      for (j <- 0 until colSpan) {
        spans(startRow + i)(startCol + j)(CellSpan.COL) = jj
        spans(startRow + i)(startCol + j)(CellSpan.ROW) = ii
        //System.out.println("r " +ii +"  c " +jj);
        jj -= 1
      }
      ii -= 1
    }
    
    spans(startRow)(startCol)(CellSpan.COL) = colSpan
    spans(startRow)(startCol)(CellSpan.ROW) = rowSpan

  }

  def split(row: Int, column: Int) {
    if (isOutOfBounds(row, column)) {
      return
    }
    val columnSpan = spans(row)(column)(CellSpan.COL)
    val rowSpan = spans(row)(column)(CellSpan.ROW)
    for (i <- 0 until rowSpan) {
      for (j <- 0 until columnSpan) {
        spans(row + i)(column + j)(CellSpan.COL) = 1
        spans(row + i)(column + j)(CellSpan.ROW) = 1
      }
    }
  }

  // ----- ColoredCell

  def getForeground(row: Int, col: Int): Color = {
    if (isOutOfBounds(row, col)) {
      return null
    }
    val attr = attrs(row)(col)
    if (attr == null) {
      null
    } else {
      attr.foreground
    }
  }

  def setForeground(color: Color, row: Int, col: Int) {
    if (isOutOfBounds(row, col)) {
      return
    }
    var attr = attrs(row)(col)
    if (attr == null) {
      attr = new Attr
      attrs(row)(col) = attr
    }
    attr.foreground = color
  }

  def setForeground(color: Color, rows: Array[Int], cols: Array[Int]) {
    if (isOutOfBounds(rows, cols)) {
      return
    }
    setAttributes(AttrType.Foreground, color, rows, cols)
  }

  def getBackground(row: Int, col: Int): Color = {
    if (isOutOfBounds(row, col)) {
      return null
    }
    val attr = attrs(row)(col)
    if (attr != null) {
      attr.background
    } else null
  }

  def setBackground(color: Color, row: Int, col: Int) {
    if (isOutOfBounds(row, col)) {
      return
    }
    var attr = attrs(row)(col)
    if (attr == null) {
      attr = new Attr()
      attrs(row)(col) = attr
    }
    attr.background = color
  }

  def setBackground(color: Color, rows: Array[Int], cols: Array[Int]) {
    if (isOutOfBounds(rows, cols)) {
      return
    }
    setAttributes(AttrType.Background, color, rows, cols)
  }

  // ----- CellFont

  def getFont(row: Int, col: Int): Font = {
    if (isOutOfBounds(row, col)) {
      return null
    }
    var attr = attrs(row)(col)
    if (attr != null) {
      attr.font
    } else null
  }

  def setFont(font: Font, row: Int, col: Int) {
    if (isOutOfBounds(row, col)) {
      return
    }
    var attr = attrs(row)(col)
    if (attr == null) {
      attr = new Attr
      attrs(row)(col) = attr
    }
    attr.font = font
  }

  def setFont(font: Font, rows: Array[Int], cols: Array[Int]) {
    if (isOutOfBounds(rows, cols)) {
      return
    }
    setAttributes(AttrType.Font, font, rows, cols)
  }

  def getHorizontalAlignment(row: Int, col: Int): Int = {
    if (isOutOfBounds(row, col)) {
      return SwingConstants.LEADING
    }
    val attr = attrs(row)(col)
    if (attr == null) {
      SwingConstants.LEADING
    } else {
      attr.horizontalAlignment
    }
  }

  def setHorizontalAlignment(horizontalAlignment: Int, row: Int, col: Int) {
    if (isOutOfBounds(row, col)) {
      return
    }
    var attr = attrs(row)(col)
    if (attr == null) {
      attr = new Attr
      attrs(row)(col) = attr
    }
    attr.horizontalAlignment = horizontalAlignment
  }

  private def setAttributes(tpe: AttrType, value: Object, rows: Array[Int], cols: Array[Int]) {
    for (i <- 0 until rows.length) {
      val row = rows(i)
      for (j <- 0 until cols.length) {
        val col = cols(j)
        var attr = attrs(row)(col)
        if (attr == null) {
          attr = new Attr
          attrs(row)(col) = attr
        }
        
        tpe match {
          case AttrType.Foreground =>
            attr.foreground = value.asInstanceOf[Color]
          case AttrType.Background =>
            attr.background = value.asInstanceOf[Color]
          case AttrType.Font =>
            attr.font = value.asInstanceOf[Font]
          case AttrType.HorizontalAlignment =>
            attr.horizontalAlignment = value.asInstanceOf[Int]
          case AttrType.VerticalAlignment =>
            attr.verticalAlignment = value.asInstanceOf[Int]
        }
      }
    }
  }
}
