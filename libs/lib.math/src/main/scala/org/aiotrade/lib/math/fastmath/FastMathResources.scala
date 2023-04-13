package org.aiotrade.lib.math.fastmath

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer

/**
 * Utility class for saving and loading tabulated data used by
 * {@link FastMath}.
 *
 * Ported by Caoyuan Deng from Java version at org.apache.commons.math3.util
 */
protected object FastMathResources {
  /**
   * Resource directory. Assuming that this class and the resource files
   * are located in the same package as "FastMath".
   */
  private val RES_DIR = "data/" + this.getClass.getPackage.getName.replace('.', '/') + "/"
  /** File resource prefix. */
  private val RES_PREFIX = RES_DIR + "FastMath__"
  /** Resource basename for "EXP_INT_TABLE_A" and "EXP_INT_TABLE_B". */
  private val EXP_INT = "exp_int"
  /** Resource basename for "EXP_FRAC_TABLE_A" and "EXP_FRAC_TABLE_B". */
  private val EXP_FRAC = "exp_frac"
  /** Resource basename for "LN_MANT". */
  private val LN_MANT = "ln_mant"
  /** Number of bytes in a "double". */
  private val BYTES_IN_DOUBLE: Int = (java.lang.Double.SIZE / java.lang.Byte.SIZE).toInt

  /**
   * Compute and save all the resources.
   */
  def createAll {
    // Create resource directory.
    val resDir = new File(RES_DIR)
    if (resDir.exists) {
      if (!resDir.isDirectory) {
        throw new RuntimeException(RES_DIR + " is not directory.")
      }
    } else {
      try {
        resDir.mkdirs
      } catch {
        case ex: SecurityException => throw ex
      }
    }

    // "EXP_INT" tables.
    val expIntA = new Array[Double](FastMath.EXP_INT_TABLE_LEN)
    val expIntB = new Array[Double](FastMath.EXP_INT_TABLE_LEN)

    val tmp = new Array[Double](2)
    val recip = new Array[Double](2)

    var i = 0
    while (i < FastMath.EXP_INT_TABLE_MAX_INDEX) {
      FastMathCalc.expint(i, tmp)
      expIntA(i + FastMath.EXP_INT_TABLE_MAX_INDEX) = tmp(0)
      expIntB(i + FastMath.EXP_INT_TABLE_MAX_INDEX) = tmp(1)

      if (i != 0) {
        // Negative integer powers.
        FastMathCalc.splitReciprocal(tmp, recip)
        expIntA(FastMath.EXP_INT_TABLE_MAX_INDEX - i) = recip(0)
        expIntB(FastMath.EXP_INT_TABLE_MAX_INDEX - i) = recip(1)
      }
      i += 1
    }

    saveTable2d(EXP_INT, Array(expIntA, expIntB))

    // "EXP_FRAC" tables.
    val expFracA = new Array[Double](FastMath.EXP_FRAC_TABLE_LEN)
    val expFracB = new Array[Double](FastMath.EXP_FRAC_TABLE_LEN)

    i = 0
    while (i < FastMath.EXP_FRAC_TABLE_LEN) {
      FastMathCalc.slowexp(i / 1024d, tmp) // TWO_POWER_10
      expFracA(i) = tmp(0)
      expFracB(i) = tmp(1)
      i += 1
    }

    saveTable2d(EXP_FRAC, Array(expFracA, expFracB))

    // "LN_MANT" table.
    val lnMant = new Array[Array[Double]](FastMath.LN_MANT_LEN)

    i = 0
    while (i < FastMath.LN_MANT_LEN) {
      val d = java.lang.Double.longBitsToDouble((i.toLong << 42) | 0x3ff0000000000000L)
      lnMant(i) = FastMathCalc.slowLog(d)
      i += 1
    }

    saveTable2d(LN_MANT, transpose(lnMant))
  }

  /**
   * Load "EXP_INT" tables.
   * "EXP_INT_TABLE_A" is at index 0.
   * "EXP_INT_TABLE_B" is at index 1.
   *
   * @return the retrieved data.
   */
  def loadExpInt: Array[Array[Double]] = {
    loadTable2d(EXP_INT, 2, FastMath.EXP_INT_TABLE_LEN)
  }

  /**
   * Load "EXP_FRAC" tables.
   * "EXP_FRAC_TABLE_A" is at index 0.
   * "EXP_FRAC_TABLE_B" is at index 1.
   *
   * @return the retrieved data.
   */
  def loadExpFrac: Array[Array[Double]] = {
    loadTable2d(EXP_FRAC, 2, FastMath.EXP_FRAC_TABLE_LEN)
  }

  /**
   * Load "LN_MANT".
   *
   * @return the retrieved data.
   */
  def loadLnMant: Array[Array[Double]] = {
    transpose(loadTable2d(LN_MANT, 2, FastMath.LN_MANT_LEN))
  }

  /**
   * @param name Basename of the resource.
   * @return an output stream.
   * @throws FileNotFoundException if the file cannot be opened.
   */
  @throws(classOf[FileNotFoundException])
  private def out(name: String): DataOutputStream = {
    val fullName = RES_PREFIX + name
    new DataOutputStream(new BufferedOutputStream(new FileOutputStream(fullName)))
  }

  /**
   * @param name Basename of the resource.
   * @param data Data to be stored.
   */
  private def saveTable1d(name: String, data: Array[Double]) {
    val len = data.length

    try {
      val dos = out(name)

      var i = 0
      while (i < len) {
        dos.writeDouble(data(i))
        i += 1
      }

      dos.close
    } catch {
      case ex: IOException => throw ex
    }
  }

  /**
   * @param name Basename of the resource.
   * @param data Data to be stored.
   */
  private def saveTable2d(name: String , data: Array[Array[Double]] ) {
    val len = data.length
    val rowLen = data(0).length

    try {
      val dos = out(name)

      var i = 0
      while (i < len) {
        var j = 0
        while (j < rowLen) {
          dos.writeDouble(data(i)(j))
          j += 1
        }
        i += 1
      }

      dos.close
    } catch {
      case ex: IOException => throw ex
    }
  }

  /**
   * @param name Basename of the resource.
   * @return an input stream.
   * @throws FileNotFoundException if the resource cannot be accessed.
   */
  @throws(classOf[FileNotFoundException])
  private def in(name: String): DataInputStream = {
    val fullName = "/" + RES_PREFIX + name
    val in = this.getClass.getResourceAsStream(fullName)
    new DataInputStream(new BufferedInputStream(in))
  }

  /**
   * @param name Basename of the resource.
   * @param len Size of the data.
   * @return the retrieved data.
   */
  private def loadTable1d(name: String, len: Int): Array[Double] = {
    try {
      val dis = in(name)

      val data = new Array[Double](len)
      var i = 0
      while (i < len) {
        data(i) = dis.readDouble
        i += 1
      }

      dis.close
      data
    } catch {
      case ex: IOException => throw ex
    }
  }

  /**
   * @param name Basename of the resource.
   * @param len Size of the table.
   * @param rowLen Size of each row of the table.
   * @return the retrieved data.
   */
  private def loadTable2d(name: String, len: Int, rowLen: Int): Array[Array[Double]] = {
    try {
      val dis = in(name)
      val b = Array.ofDim[Byte](BYTES_IN_DOUBLE * rowLen)
      val data = Array.ofDim[Double](len, rowLen)
      val bBuf = ByteBuffer.wrap(b)

      var i = 0
      while (i < len) {
        dis.readFully(b)
        val dBuf = bBuf.asDoubleBuffer
        var j = 0
        while (j < rowLen) {
          data(i)(j) = dBuf.get
          j += 1
        }
        i += 1
      }

      dis.close
      data
    } catch {
      case ex: IOException => throw ex
    }
  }

  /**
   * Transposes a two-dimensional array: The number of rows becomes the
   * number of columns and vice-versa.
   * The array must be rectangular (same number of colums in each row).
   *
   * @param data Array to be transposed.
   * @return the transposed array.
   */
  private def transpose(data: Array[Array[Double]]): Array[Array[Double]] = {
    val rowLen = data.length
    val len = data(0).length
    val tData = Array.ofDim[Double](len, rowLen)

    var i = 0
    while (i < len) {
      var j = 0
      while (j < rowLen) {
        tData(i)(j) = data(j)(i)
        j += 1
      }
      i += 1
    }

    tData
  }
}