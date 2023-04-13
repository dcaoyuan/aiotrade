package org.aiotrade.lib.dbfdriver

import java.io.DataOutput
import java.io.IOException
import java.nio.ByteBuffer


class DBFField {
  /* Field struct variables start here */
  private var _name = new Array[Byte](11)   /* 0-10*/
  private var _dataType: Byte = _           /* 11 */
  private var reserv1: Int = _              /* 12-15 */
  private var _length: Int = _              /* 16 */
  private var _decimalCount: Byte = _       /* 17 */
  private var reserv2: Short = _            /* 18-19 */
  private var workAreaId: Byte = _          /* 20 */
  private var reserv3: Short = _            /* 21-22 */
  private var setFieldsFlag: Byte = _       /* 23 */
  private var reserv4 = new Array[Byte](7)  /* 24-30 */
  private var indexFieldFlag: Byte = _      /* 31 */
  /* Field struct variables end here */

  /* other class variables */
  private var nameNullIndex = 0

  /**
   * load a DBFField object from the data read from the given DataInputStream.
   *
   * The data in the DataInputStream object is supposed to be organised correctly
   * and the stream "pointer" is supposed to be positioned properly.
   *
   * @param in DataInputStream
   * @return Returns the created DBFField object.
   * @throws IOException If any stream reading problems occures.
   */
  @throws(classOf[IOException])
  def read(in: ByteBuffer): DBFField = {
    val t_byte = in.get /* 0 */
    if (t_byte == 0x0D.toByte) {
      return null
    }

    in.get(this._name, 1, 10)	/* 1-10 */
    this._name(0) = t_byte
    var i = 0
    var break = false
    while (i < this._name.length && !break) {
      if (this._name(i) == 0) {
        this.nameNullIndex = i
        break = true
      }
      i += 1
    }

    this.dataType = in.get                         /* 11 */
    this.reserv1 = Utils.readLittleEndianInt(in)   /* 12-15 */
    // read byte as unsigne to int with & 0xFF
    this.length = in.get & 0xFF                    /* 16 */
    this.decimalCount = in.get                     /* 17 */
    this.reserv2 = Utils.readLittleEndianShort(in) /* 18-19 */
    this.workAreaId = in.get                       /* 20 */
    this.reserv3 = Utils.readLittleEndianShort(in) /* 21-22 */
    this.setFieldsFlag = in.get                    /* 23 */
    in.get(this.reserv4)                           /* 24-30 */
    this.indexFieldFlag = in.get                   /* 31 */

    this
  }

  /**
   * Writes the content of DBFField object into the stream as per
   * DBF format specifications.
   *
   * @param os OutputStream
   * @throws IOException if any stream related issues occur.
   */
  @throws(classOf[IOException])
  def write(out: DataOutput) {
    out.write(_name)              /* 0-10 */
    out.write(new Array[Byte](11 - _name.length))

    out.writeByte(_dataType)      /* 11 */
    out.writeInt(0x00)            /* 12-15 */
    out.writeByte(_length)         /* 16 */
    out.writeByte(_decimalCount)  /* 17 */
    out.writeShort(0x00)          /* 18-19 */
    out.writeByte(0x00)           /* 20 */
    out.writeShort(0x00)          /* 21-22 */
    out.writeByte(0x00)           /* 23 */
    out.write(new Array[Byte](7)) /* 24-30*/
    out.writeByte(0x00)           /* 31 */
  }

  /**
   Sets the name of the field.

   @param name of the field as String.
   @since 0.3.3.1
   */
  def name = new String(this._name, 0, nameNullIndex)
  def name_=(name: String) {
    if (name == null) {
      throw new IOException("Field name cannot be null")
    }

    if (name.length == 0 || name.length > 10) {
      throw new IOException("Field name should be of length 0-10")
    }

    _name = name.getBytes
    nameNullIndex = this._name.length
  }

  /**
   * Sets the data type of the field.
   *
   * @param type of the field. One of the following:<br>
   * C, L, N, F, D, M,T(DateTime),I(Int)
   */
  def dataType = _dataType
  def dataType_=(dataType: Byte) {
    dataType match {
      case 'D' => _length = 8
      case 'T' => _length = 17
      case 'C' | 'L' | 'N' | 'F' | 'M' | 'I' =>
      case x => throw new IOException("Unknown data type: " + x.toChar)
    }
    
    _dataType = dataType
  }

  /**
   * Length of the field.
   * This method should be called before calling setDecimalCount().
   *
   * @param Length of the field as int.
   */
  def length = _length
  def length_=(length: Int) {
    if (length <= 0) {
      throw new IOException("Field length should be a positive number");
    }

    if (_dataType == 'D') {
      _length = 8
    }
    if (_dataType == 'T') {
      _length = 17
    }

    _length = length
  }

  /**
   * Sets the decimal place size of the field.
   * Before calling this method the size of the field
   * should be set by calling setFieldLength().
   *
   * @param Size of the decimal field.
   */
  def decimalCount = _decimalCount
  def decimalCount_=(count: Int) {
    if (count < 0) {
      throw new IOException("Decimal length should be a positive number")
    }

    if (count > _length) {
      throw new IOException("Decimal length should be less than field length")
    }

    _decimalCount = count.toByte
  }

  override def toString = {
    val sb = new StringBuilder
    sb.append(name).append(":").append(dataType.toChar).append("(").append(length).append(",").append(decimalCount).append(")")
    sb.toString
  }
}

object DBFField {

  /**
   * Creates a DBFField object from the data read from the given DataInputStream.
   *
   * The data in the DataInputStream object is supposed to be organised correctly
   * and the stream "pointer" is supposed to be positioned properly.
   *
   * @param in DataInputStream
   * @return Returns the created DBFField object.
   * @throws IOException If any stream reading problems occures.
   */
  @throws(classOf[IOException])
  def read(in: ByteBuffer): DBFField = (new DBFField).read(in)

  def apply(name: String, dataType: Char, length: Int, decimalCount: Int) = {
    val field = new DBFField

    field.name = name
    field.dataType = dataType.toByte
    field.length = length
    field.decimalCount = decimalCount

    field
  }
}
