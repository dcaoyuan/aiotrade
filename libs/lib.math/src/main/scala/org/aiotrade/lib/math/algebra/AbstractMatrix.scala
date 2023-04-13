package org.aiotrade.lib.math.algebra

import org.aiotrade.lib.math.CardinalityException
import org.aiotrade.lib.math.IndexException
import org.aiotrade.lib.math.Functions
import scala.collection.mutable


/** A few universal implementations of convenience functions */
abstract class AbstractMatrix protected (protected var rows: Int, protected var columns: Int) extends Matrix {

  protected var columnLabelBindings: collection.Map[String, Int] = _
  protected var rowLabelBindings: collection.Map[String, Int] = _

  def columnSize: Int = {
    columns
  }

  def rowSize: Int = {
    rows
  }

  def iterator: Iterator[MatrixSlice] = {
    iterateAll
  }

  def iterateAll: Iterator[MatrixSlice] = {
    new Iterator[MatrixSlice]() {
      private var slice: Int = 0
      
      def hasNext = slice < numSlices
      def next: MatrixSlice = {
        if (hasNext) {
          val i = slice
          slice += 1
          new MatrixSlice(viewRow(i), i)
        } else {
          null
        }
      }
    }
  }

  /**
   * Abstracted out for the iterator
   * @return numRows() for row-based iterator, numColumns() for column-based.
   */
  def numSlices: Int = {
    return numRows
  }

  def get(rowLabel: String, columnLabel: String): Double = {
    if (columnLabelBindings == null || rowLabelBindings == null) {
      throw new IllegalStateException("Unbound label")
    }
    (rowLabelBindings.get(rowLabel), columnLabelBindings.get(columnLabel)) match {
      case (Some(row), Some(col)) => get(row, col)
      case _ => throw new IllegalStateException("Unbound label")
    }
  }

  def getColumnLabelBindings: collection.Map[String, Int] = {
    columnLabelBindings
  }

  def getRowLabelBindings: collection.Map[String, Int] = {
    rowLabelBindings
  }

  def set(rowLabel: String, rowData: Array[Double]) {
    if (columnLabelBindings == null) {
      throw new IllegalStateException("Unbound label")
    }
    rowLabelBindings.get(rowLabel) match {
      case Some(row) => set(row, rowData)
      case None => throw new IllegalStateException("Unbound label")
    }    
  }

  def set(rowLabel: String, row: Int, rowData: Array[Double]) {
    if (rowLabelBindings == null) {
      rowLabelBindings = new mutable.HashMap[String, Int]()
    }
    rowLabelBindings += (rowLabel -> row)
    set(row, rowData)
  }

  def set(rowLabel: String, columnLabel: String, value: Double) {
    if (columnLabelBindings == null || rowLabelBindings == null) {
      throw new IllegalStateException("Unbound label")
    }
    (rowLabelBindings.get(rowLabel), columnLabelBindings.get(columnLabel)) match {
      case (Some(row), Some(col)) => set(row, col, value)
      case _ => throw new IllegalStateException("Unbound label")
    }
  }

  def set(rowLabel: String, columnLabel: String, row: Int, column: Int, value: Double) {
    if (rowLabelBindings == null) {
      rowLabelBindings = new mutable.HashMap[String, Int]()
    }
    rowLabelBindings += (rowLabel -> row)
    if (columnLabelBindings == null) {
      columnLabelBindings = new mutable.HashMap[String, Int]()
    }
    columnLabelBindings += (columnLabel -> column)

    set(row, column, value)
  }

  def setColumnLabelBindings(bindings: collection.Map[String, Int]) {
    columnLabelBindings = bindings
  }

  def setRowLabelBindings(bindings: collection.Map[String, Int]) {
    rowLabelBindings = bindings
  }


  def numRows: Int = {
    rowSize
  }

  def numCols: Int = {
    columnSize
  }

  def asFormatString: String = {
    toString
  }

  def assign(value: Double): Matrix = {
    val rows = rowSize
    val columns = columnSize
    var row = 0
    while (row < rows) {
      var col = 0
      while (col < columns) {
        this(row, col) = value
        col += 1
      }
      row += 1
    }
    this
  }

  def assign(values: Array[Array[Double]]): Matrix = {
    val rows = rowSize
    if (rows != values.length) {
      throw new CardinalityException(rows, values.length)
    }
    val columns = columnSize
    var row = 0
    while (row < rows) {
      if (columns == values(row).length) {
        var col = 0
        while (col < columns) {
          this(row, col) = values(row)(col)
          col += 1
        }
      } else {
        throw new CardinalityException(columns, values(row).length)
      }
      row += 1
    }
    this
  }

  def assign(other: Matrix, function: (Double, Double) => Double): Matrix = {
    val rows = rowSize
    if (rows != other.rowSize) {
      throw new CardinalityException(rows, other.rowSize)
    }
    val columns = columnSize
    if (columns != other.columnSize) {
      throw new CardinalityException(columns, other.columnSize)
    }
    var row = 0
    while (row < rows) {
      var col = 0
      while (col < columns) {
        this(row, col) = function(this(row, col), other(row, col))
        col += 1
      }
      row += 1
    }
    this
  }

  def assign(other: Matrix): Matrix = {
    val rows = rowSize;
    if (rows != other.rowSize) {
      throw new CardinalityException(rows, other.rowSize)
    }
    val columns = columnSize;
    if (columns != other.columnSize) {
      throw new CardinalityException(columns, other.columnSize)
    }
    var row = 0
    while (row < rows) {
      var col = 0
      while (col < columns) {
        this(row, col) = other(row, col)
        col += 1
      }
      row += 1
    }
    this;
  }

  def assign(function: Double => Double): Matrix = {
    val rows = rowSize
    val columns = columnSize
    var row = 0
    while (row < rows) {
      var col = 0
      while (col < columns) {
        this(row, col) = function(this(row, col))
        col += 1
      }
      row += 1
    }
    this;
  }

  /**
   * Collects the results of a function applied to each row of a matrix.
   *
   * @param f The function to be applied to each row.
   * @return The vector of results.
   */
  def aggregateRows(f: Vector => Double): Vector = {
    val r = DenseVector(numRows)
    val n = numRows
    var row = 0
    while (row < n) {
      r.set(row, f(viewRow(row)))
      row += 1
    }
    r
  }

  /**
   * Returns a view of a row.  Changes to the view will affect the original.
   * @param row  Which row to return.
   * @return A vector that references the desired row.
   */
  def viewRow(row: Int): Vector = {
    MatrixVectorView(this, row, 0, 0, 1)
  }


  /**
   * Returns a view of a row.  Changes to the view will affect the original.
   * @param column Which column to return.
   * @return A vector that references the desired column.
   */
  def viewColumn(column: Int): Vector = {
    MatrixVectorView(this, 0, column, 1, 0)
  }

  /**
   * Provides a view of the diagonal of a matrix.
   */
  def viewDiagonal: Vector = {
    MatrixVectorView(this, 0, 0, 1, 1)
  }

  /**
   * Collects the results of a function applied to each element of a matrix and then aggregated.
   *
   * @param combiner A function that combines the results of the mapper.
   * @param mapper   A function to apply to each element.
   * @return The result.
   */
  def aggregate(combiner: (Double, Double) => Double, mapper: Double => Double): Double = {
    aggregateRows((v: Vector) => v.aggregate(combiner, mapper)).aggregate(combiner, Functions.IDENTITY)
  }

  /**
   * Collects the results of a function applied to each column of a matrix.
   *
   * @param f The function to be applied to each column.
   * @return The vector of results.
   */
  def aggregateColumns(f: Vector => Double): Vector = {
    val r = DenseVector(numCols)
    var col = 0
    while (col < numCols) {
      r.set(col, f(viewColumn(col)))
      col += 1
    }
    r
  }

  def determinant: Double = {
    val rows = rowSize
    val columns = columnSize
    if (rows != columns) {
      throw new CardinalityException(rows, columns)
    }

    if (rows == 2) {
      return this(0, 0) * this(1, 1) - this(0, 1) * this(1, 0)
    } else {
      // TODO: this really should just be one line:
      // TODO: new CholeskyDecomposition(this).getL().viewDiagonal().aggregate(Functions.TIMES)
      var sign = 1
      var ret = 0.0

      var i = 0
      while (i < columns) {
        val minor = DenseMatrix(rows - 1, columns - 1)
        var j = 1
        while (j < rows) {
          var flag = false /* column offset flag */
          var k = 0
          while (k < columns) {
            if (k == i) {
              flag = true
            } else {
              minor.set(j - 1, if (flag) k - 1 else k, this(j, k))
            }
            k += 1
          }
          j += 1
        }
        ret += this(0, i) * sign * minor.determinant
        sign *= -1
        
        i += 1
      }

      ret
    }

  }

  override 
  def clone: Matrix = {
    var x: AbstractMatrix = null
    try {
      x = super.clone.asInstanceOf[AbstractMatrix]
    } catch {
      case e: CloneNotSupportedException =>
        throw new IllegalStateException(e) // can't happen
    }
    if (rowLabelBindings != null) {
      x.rowLabelBindings = new mutable.HashMap[String, Int]() ++= rowLabelBindings
    }
    if (columnLabelBindings != null) {
      x.columnLabelBindings = new mutable.HashMap[String, Int]() ++= columnLabelBindings
    }
    x
  }

  def divide(x: Double): Matrix = {
    val result = like
    var row = 0
    while (row < rowSize) {
      var col = 0
      while (col < columnSize) {
        result(row, col) = this(row, col) / x
        col += 1
      }
      row += 1
    }
    result
  }

  def get(row: Int, column: Int): Double = {
    if (row < 0 || row >= rowSize) {
      throw new IndexException(row, rowSize)
    }
    if (column < 0 || column >= columnSize) {
      throw new IndexException(column, columnSize)
    }
    this(row, column)
  }

  def minus(other: Matrix): Matrix = {
    val rows = rowSize;
    if (rows != other.rowSize) {
      throw new CardinalityException(rows, other.rowSize)
    }
    val columns = columnSize
    if (columns != other.columnSize) {
      throw new CardinalityException(columns, other.columnSize)
    }
    val result = like
    var row = 0
    while (row < rows) {
      var col = 0
      while (col < columns) {
        result(row, col) = this(row, col) - other(row, col)
        col += 1
      }
      row += 1
    }
    result
  }

  def plus(x: Double): Matrix = {
    val result = like
    val rows = rowSize
    val columns = columnSize
    var row = 0
    while (row < rows) {
      var col = 0
      while (col < columns) {
        result(row, col) = this(row, col) + x
        col += 1
      }
      row += 1
    }
    result
  }

  def plus(other: Matrix): Matrix = {
    val rows = rowSize
    if (rows != other.rowSize) {
      throw new CardinalityException(rows, other.rowSize)
    }
    val columns = columnSize 
    if (columns != other.columnSize) {
      throw new CardinalityException(columns, other.columnSize)
    }
    val result = like
    var row = 0
    while (row < rows) {
      var col = 0
      while (col < columns) {
        result(row, col) = this(row, col) + other(row, col)
        col += 1
      }
      row += 1
    }
    result
  }

  def set(row: Int, column: Int, value: Double) {
    if (row < 0 || row >= rowSize) {
      throw new IndexException(row, rowSize)
    }
    if (column < 0 || column >= columnSize) {
      throw new IndexException(column, columnSize)
    }
    this(row, column) = value
  }

  def set(row: Int, data: Array[Double]) {
    val columns = columnSize
    if (columns < data.length) {
      throw new CardinalityException(columns, data.length)
    }
    val rows = rowSize
    if (row < 0 || row >= rows) {
      throw new IndexException(row, rowSize)
    }
    var i = 0
    while (i < columns) {
      this(row, i) = data(i)
      i += 1
    }
  }

  def times(x: Double): Matrix = {
    val result = like
    val rows = rowSize
    val columns = columnSize
    var row = 0
    while (row < rows) {
      var col = 0
      while (col < columns) {
        result(row, col) = this(row, col) * x
        col += 1
      }
      row += 1
    }
    result;
  }

  def times(other: Matrix): Matrix = {
    val columns = columnSize
    if (columns != other.rowSize) {
      throw new CardinalityException(columns, other.rowSize)
    }
    val rows = rowSize
    val otherColumns = other.columnSize
    val result = like(rows, otherColumns)
    var row = 0
    while (row < rows) {
      var col = 0
      while (col < otherColumns) {
        var sum = 0.0
        var k = 0
        while (k < columns) {
          sum += this(row, k) * other(k, col)
          k += 1
        }
        result(row, col) = sum
        col += 1
      }
      row += 1
    }
    result;
  }

  def times(v: Vector): Vector = {
    val columns = columnSize
    if (columns != v.size) {
      throw new CardinalityException(columns, v.size)
    }
    val rows = rowSize
    val w = DenseVector(rows)
    var row = 0
    while (row < rows) {
      w(row) = v.dot(viewRow(row))
      row += 1
    }
    w
  }

  def timesSquared(v: Vector): Vector = {
    val columns = columnSize
    if (columns != v.size) {
      throw new CardinalityException(columns, v.size)
    }
    val rows = rowSize
    val w = DenseVector(columns)
    var i = 0
    while (i < rows) {
      val xi = viewRow(i)
      val d = xi.dot(v)
      if (d != 0.0) {
        w.assign(xi, Functions.plusMult(d))
      }
      i += 1
    }
    w
  }

  def transpose: Matrix = {
    val rows = rowSize
    val columns = columnSize
    val result = like(columns, rows)
    var row = 0
    while (row < rows) {
      var col = 0
      while (col < columns) {
        result(col, row) = this(row, col)
        col += 1
      }
      row += 1
    }
    result
  }

  def viewPart(rowOffset: Int, rowsRequested: Int, columnOffset: Int, columnsRequested: Int): Matrix = {
    viewPart(Array(rowOffset, columnOffset), Array(rowsRequested, columnsRequested))
  }

  def zSum: Double = {
    var result = 0.0
    var row = 0
    while (row < rowSize) {
      var col = 0
      while (col < columnSize) {
        result += this(row, col)
        col += 1
      }
      row += 1
    }
    result
  }

  def getNumNondefaultElements: Array[Int] = {
    Array(rowSize, columnSize)
  }

  protected class TransposeViewVector(matrix: Matrix, offset: Int, rowToColumn: Boolean) extends AbstractVector(if (rowToColumn) matrix.numRows else matrix.numCols) {

    private val transposeOffset = offset
    private val numCols = if (rowToColumn) matrix.numRows else matrix.numCols

    protected def this(m: Matrix, offset: Int) = {
      this(m, offset, true)
    }

    override 
    def clone: Vector = {
      val v = DenseVector(size)
      v.assign(this, Functions.PLUS)
      v
    }

    override 
    def isDense: Boolean = {
      true
    }

    override 
    def isSequentialAccess: Boolean = {
      true
    }

    override 
    protected[algebra] def matrixLike(rows: Int, columns: Int): Matrix = {
      matrix.like(rows, columns)
    }

    override 
    def iterator: Iterator[Element] = {
      return new Iterator[Element]() {
        private var index: Int = _
        def hasNext = index < size
        def next: Element = {
          if (hasNext) {
            val i = index
            index += 1
            getElement(i)
          } else {
            null
          }
        }
      }
    }

    /**
     * Currently delegates to {@link #iterator()}.
     * TODO: This could be optimized to at least skip empty rows if there are many of them.
     * @return an iterator (currently dense).
     */
    override 
    def iterateNonZero: Iterator[Element] = {
      iterator
    }

    override 
    def getElement(i: Int): Element = {
      new Element() {
        def index: Int = i
        
        def get: Double = apply(i)
        def set(value: Double) {
          update(i, value)
        }
      }
    }

    override 
    def apply(index: Int): Double = {
      val v = if (rowToColumn) matrix.viewColumn(index) else matrix.viewRow(index)
      if (v == null) 0.0 else v(transposeOffset)
    }

    override 
    def update(index: Int, value: Double) {
      var v = if (rowToColumn) matrix.viewColumn(index) else matrix.viewRow(index)
      if (v == null) {
        v = newVector(numCols)
        if (rowToColumn) {
          matrix.assignColumn(index, v)
        } else {
          matrix.assignRow(index, v)
        }
      }
      v(transposeOffset) = value
    }

    protected def newVector(cardinality: Int): Vector = {
      DenseVector(cardinality)
    }

    override 
    def like(): Vector = {
      DenseVector(size)
    }

    def like(cardinality: Int): Vector = {
      DenseVector(cardinality)
    }

    /**
     * TODO: currently I don't know of an efficient way to getVector this value correctly.
     *
     * @return the number of nonzero entries
     */
    override 
    def getNumNondefaultElements: Int = {
      size
    }
  }

}

object AbstractMatrix {
  // index into Int[2] for column value
  val COL = 1

  // index into Int[2] for row value
  val ROW = 0
}