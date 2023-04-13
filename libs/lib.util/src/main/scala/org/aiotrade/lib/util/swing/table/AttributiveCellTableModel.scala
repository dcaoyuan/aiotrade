package org.aiotrade.lib.util.swing.table

import java.util.Vector
import javax.swing.event.TableModelEvent
import javax.swing.table.DefaultTableModel

/**
 * @version 1.0 11/22/98
 */
object AttributiveCellTableModel {

  def apply(data: Vector[_], colNames: Vector[_]) = {
    new AttributiveCellTableModel(data, colNames)
  }

  def apply(colNames: Vector[_], nRows: Int): AttributiveCellTableModel = {
    new AttributiveCellTableModel(colNames, nRows)
  }

  def apply(nRows: Int, nCols: Int): AttributiveCellTableModel = {
    new AttributiveCellTableModel(nRows, nCols)
  }

  def apply(data: Array[_], colNames: Array[_]) = {
    new AttributiveCellTableModel(data, colNames)
  }

  def apply(colNames: Array[_], nRows: Int): AttributiveCellTableModel = {
    new AttributiveCellTableModel(colNames, nRows)
  }

  def apply(): AttributiveCellTableModel = {
    new AttributiveCellTableModel(0, 0)
  }

  private def prepareDataVector(nRows: Int): Vector[_] = {
    val dataVector = new Vector[Object](nRows)
    dataVector.setSize(nRows)
    dataVector
  }

  private def prepareColNames(nCols: Int): Vector[_] = {
    val colNames = new Vector[Object](nCols)
    colNames.setSize(nCols)
    colNames
  }

  /**
   * Returns a vector that contains the same objects as the array.
   * @param anArray  the array to be converted
   * @return  the new vector; if <code>anArray</code> is <code>null</code>,
   *                          returns <code>null</code>
   */
  private def arrayToVector(arr: Array[_]): Vector[_] = {
    if (arr == null) {
      return null
    }

    val vector = new Vector[Object](arr.length)
    arr foreach {
      case x: Array[_] => vector.addElement(arrayToVector(x))
      case x: Object   => vector.addElement(x)
      case null => vector.addElement(null)
    }

    vector
  }

  private def nonNullVector[T](v: Vector[T]): Vector[T] = {
    if (v != null) v else new Vector[T]
  }
}

import AttributiveCellTableModel._
class AttributiveCellTableModel(data: Vector[_], colNames: Vector[_]) extends DefaultTableModel(data, colNames) {

  protected var _cellAttr: CellAttribute = _

  def this(colNames: Vector[_], nRows: Int) = {
    this(prepareDataVector(nRows), colNames)
  }

  def this(nRows: Int, nCols: Int) = {
    this(prepareColNames(nCols), nRows)
  }

  def this(data: Array[_], colNames: Array[_]) = {
    this(arrayToVector(data), arrayToVector(colNames))
  }

  def this(colNames: Array[_], nRows: Int) = {
    this(arrayToVector(colNames), nRows)
  }

  def this() = {
    this(0, 0)
  }

  override def setDataVector(data: Vector[_], colNames: Vector[_]) {
    if (data == null) {
      throw new IllegalArgumentException("setDataVector() - Null data parameter")
    }
    dataVector = data
    columnIdentifiers = nonNullVector(colNames)

    _cellAttr = new DefaultCellAttribute(dataVector.size, columnIdentifiers.size)

    newRowsAdded(new TableModelEvent(this, 0, getRowCount - 1,
                                     TableModelEvent.ALL_COLUMNS, TableModelEvent.INSERT))
  }

  override def addColumn(colName: Object, colData: Vector[_]) {
    if (colName == null) {
      throw new IllegalArgumentException("addColumn() - null colName")
    }
    columnIdentifiers.asInstanceOf[Vector[Object]].addElement(colName)
    var index = 0
    val enumeration = dataVector.elements
    while (enumeration.hasMoreElements) {
      val value = if (colData != null && index < colData.size) {
        colData.asInstanceOf[Vector[Object]].elementAt(index)
      } else null
      
      enumeration.nextElement.asInstanceOf[Vector[Object]].addElement(value)
      index += 1
    }

    _cellAttr.addColumn

    fireTableStructureChanged
  }

  override def addRow(rowData: Vector[_]): Unit = {
    val rowData1 = if (rowData == null) {
      new Vector[Object](getColumnCount)
    } else rowData
    
    rowData1.setSize(getColumnCount)
    
    dataVector.asInstanceOf[Vector[Object]].addElement(rowData1)

    _cellAttr.addRow

    newRowsAdded(new TableModelEvent(this, getRowCount - 1, getRowCount - 1,
                                     TableModelEvent.ALL_COLUMNS, TableModelEvent.INSERT))
  }

  override def insertRow(row: Int, rowData: Vector[_]): Unit = {
    val rowData1 = if (rowData == null) {
      new Vector[Object](getColumnCount)
    } else rowData
    
    rowData1.setSize(getColumnCount)

    dataVector.asInstanceOf[Vector[Object]].insertElementAt(rowData1, row)

    _cellAttr.insertRow(row)

    newRowsAdded(new TableModelEvent(this, row, row,
                                     TableModelEvent.ALL_COLUMNS, TableModelEvent.INSERT))
  }

  def cellAttribute: CellAttribute = {
    _cellAttr
  }

  def cellAttribute_=(cellAttr: CellAttribute): Unit = {
    val nCols = getColumnCount
    val nRows = getRowCount
    cellAttr.dim match {
      case Dim(`nRows`, `nCols`) =>
      case _ => cellAttr.dim = Dim(nRows, nCols)
    }

    _cellAttr = cellAttr
    fireTableDataChanged
  }
  override def isCellEditable(rowIndex:Int, columnIndex:Int): Boolean = {
    false
  }
  /*
   public void changeCellAttribute(int row, int column, Object command) {
   cellAtt.changeAttribute(row, column, command);
   }

   public void changeCellAttribute(int[] rows, int[] columns, Object command) {
   cellAtt.changeAttribute(rows, columns, command);
   }
   */

}

