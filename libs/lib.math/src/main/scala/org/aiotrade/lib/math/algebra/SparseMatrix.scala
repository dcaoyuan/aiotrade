package org.aiotrade.lib.math.algebra

import org.aiotrade.lib.collection.ArrayList
import org.aiotrade.lib.math.CardinalityException
import org.aiotrade.lib.math.IndexException
import scala.collection.mutable


/** 
 * Doubly sparse matrix. Implemented as a Map of RandomAccessSparseVector rows 
 * Construct a matrix of the given cardinality with the given row map
 *
 * @param rows
 *          a Map<Integer, RandomAccessSparseVector> of rows
 * @param columns
 * @param rowVectors
 */
class SparseMatrix private (_rows: Int, _columns: Int, private var rowVectors: collection.Map[Int, Vector]) extends AbstractMatrix(_rows, _columns) {
  
  override
  def clone: Matrix = {
    val x = super.clone.asInstanceOf[SparseMatrix]
    x.rowVectors = new mutable.HashMap[Int, Vector]()
    for (entry <- rowVectors) {
      x.rowVectors += (entry._1 -> entry._2.clone)
    }
    x
  }

  override
  def iterator: Iterator[MatrixSlice] = {
    val keys = new ArrayList[Int]() ++= rowVectors.keys

    new Iterator[MatrixSlice]() {
      private var slice: Int = _
      
      def hasNext = slice < rowVectors.size
      override def next: MatrixSlice = {
        if (hasNext) {
          val i = keys(slice)
          val row = rowVectors(i)
          slice += 1;
          new MatrixSlice(row, i)
        } else {
          null
        }
      }
    }
  }
  
  override
  def apply(row: Int, column: Int): Double = {
    rowVectors.get(row) match {
      case Some(r) => r(column)
      case _ => 0.0
    }
  }
  
  override
  def like(): Matrix = {
    SparseMatrix(rowSize, columnSize)
  }
  
  override
  def like(rows: Int, columns: Int): Matrix = {
    SparseMatrix(rows, columns)
  }
  
  override
  def update(row: Int, column: Int, value: Double) {
    val r = rowVectors.get(row) match {
      case Some(r) => r
      case _ => 
        val r = RandomAccessSparseVector(columnSize)
        rowVectors += (row -> r)
        r
    }
    r(column) = value
  }
  
  override
  def getNumNondefaultElements: Array[Int] = {
    val result = new Array[Int](2)
    result(AbstractMatrix.ROW) = rowVectors.size
    for (vectorEntry <- rowVectors.values) {
      result(AbstractMatrix.COL) = math.max(result(AbstractMatrix.COL), vectorEntry.getNumNondefaultElements)
    }
    result
  }
  
  override
  def viewPart(offset: Array[Int], size: Array[Int]): Matrix = {
    if (offset(AbstractMatrix.ROW) < 0) {
      throw new IndexException(offset(AbstractMatrix.ROW), rowSize)
    }
    if (offset(AbstractMatrix.ROW) + size(AbstractMatrix.ROW) > rowSize) {
      throw new IndexException(offset(AbstractMatrix.ROW) + size(AbstractMatrix.ROW), rowSize)
    }
    if (offset(AbstractMatrix.COL) < 0) {
      throw new IndexException(offset(AbstractMatrix.COL), columnSize)
    }
    if (offset(AbstractMatrix.COL) + size(AbstractMatrix.COL) > columnSize) {
      throw new IndexException(offset(AbstractMatrix.COL) + size(AbstractMatrix.COL), columnSize)
    }
    MatrixView(this, offset, size)
  }
  
  override
  def assignColumn(column: Int, other: Vector): Matrix = {
    if (rowSize != other.size) {
      throw new CardinalityException(rowSize, other.size)
    }
    if (column < 0 || column >= columnSize) {
      throw new IndexException(column, columnSize)
    }
    var row = 0
    while (row < rowSize) {
      val v = other(row)
      if (v != 0.0) {
        val r = rowVectors.get(row) match {
          case Some(x) => x
          case None => 
            val x = RandomAccessSparseVector(columnSize)
            rowVectors += (row -> x)
            x
        }
        
        r(column) = v
      }
      row += 1
    }
    this;
  }
  
  override
  def assignRow(row: Int, other: Vector): Matrix = {
    if (columnSize != other.size) {
      throw new CardinalityException(columnSize, other.size)
    }
    if (row < 0 || row >= rowSize) {
      throw new IndexException(row, rowSize)
    }
    rowVectors += (row -> other)
    this;
  }
  
  override
  def viewRow(row: Int): Vector = {
    if (row < 0 || row >= rowSize) {
      throw new IndexException(row, rowSize)
    }
    rowVectors.getOrElse(row, RandomAccessSparseVector(columnSize))
  }
  
}

/**
 * Construct a matrix with specified number of rows and columns.
 */
object SparseMatrix {
  def apply(_rows: Int, _columns: Int, _rowVectors: collection.Map[Int, RandomAccessSparseVector]) = {
    val rowVectors = new mutable.HashMap[Int, Vector]()
    for (entry <- _rowVectors) {
      rowVectors += (entry._1 -> entry._2.clone)
    }
  
    new SparseMatrix(_rows, _columns, rowVectors)
  }
  
  def apply(_rows: Int, _columns: Int) = new SparseMatrix(_rows, _columns, new mutable.HashMap[Int, RandomAccessSparseVector]())
}

