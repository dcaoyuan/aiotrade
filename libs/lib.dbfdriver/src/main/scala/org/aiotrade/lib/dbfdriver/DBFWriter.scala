package org.aiotrade.lib.dbfdriver

import java.io.DataOutput
import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.io.RandomAccessFile
import java.nio.channels.FileChannel
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import scala.collection.mutable.ArrayBuffer

/**
 An object of this class can create a DBF file.

 Create an object, <br>
 then define fields by creating DBFField objects and<br>
 add them to the DBFWriter object<br>
 add records using the addRecord() method and then<br>
 call write() method.
 */

@throws(classOf[IOException])
class DBFWriter(dbfFile: File)  {
  import DBFWriter._
  
  var charsetName = "8859_1"

  private var records = new ArrayBuffer[Array[Any]]
  private var recordCount = 0
  private var appendMode = false

  val file = new RandomAccessFile(dbfFile, "rw")

  /* before proceeding, check whether the passed in File object
   * is an empty/non-existent file or not.
   */
  val header = if (!dbfFile.exists || dbfFile.length == 0) {
    new DBFHeader
  } else {
    val fileChannel = file.getChannel
    val in = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size)
    val header = DBFHeader.read(in)
    // position file pointer at the end of the raf
    file.seek(file.length - 1) // -1 to ignore the END_OF_DATA byte at EoF

    recordCount = header.numberOfRecords
    header
  }

  /**
   * Sets fields.
   */
  @throws(classOf[IOException])
  def setFields(fields: Array[DBFField]) {
    if (header.fields != null) {
      throw new IOException("Fields has already been set")
    }

    if (fields == null || fields.length == 0) {
      throw new IOException("Should have at least one field")
    }

    var i = 0
    while (i < fields.length) {
      if (fields(i) == null) {
        throw new IOException("Field " + (i + 1) + " is null")
      }
      i += 1
    }

    header.fields = fields

    try {
      if (file != null && file.length == 0) {
        // this is a new/non-existent file. So write header before proceeding
        header.write(file)
      }
    } catch {
      case ex: IOException => throw new IOException("Error accesing file")
    }
  }

  @throws(classOf[IOException])
  def addRecord(values: Array[Any]) {
    if (header.fields == null) {
      throw new IOException("Fields should be set before adding records");
    }

    if (values == null) {
      throw new IOException("Null cannot be added as row");
    }

    if (values.length != header.fields.length) {
      throw new IOException("Invalid record. Invalid number of fields in row");
    }

    var i = 0
    while (i < header.fields.length) {
      if (values(i) != null) {
        header.fields(i).dataType match {
          case 'C' if !values(i).isInstanceOf[String] =>
            throw new IOException("Invalid value for field " + i)
          case 'L' if !values(i).isInstanceOf[Boolean] =>
            throw new IOException("Invalid value for field " + i)
          case 'N' if !values(i).isInstanceOf[Double] =>
            throw new IOException("Invalid value for field " + i)
          case 'D' if !values(i).isInstanceOf[Date] =>
            throw new IOException("Invalid value for field " + i)
          case 'F' if !values(i).isInstanceOf[Double] =>
            throw new IOException("Invalid value for field " + i)
          case 'I' if !values(i).isInstanceOf[Int] =>
            throw new IOException("Invalid value for field " + i)
          case 'T' if !values(i).isInstanceOf[Date] =>
            throw new IOException("Invalid value for field " + i)
          case _ => 
        }
      }
      i += 1
    }

    if (file == null) {
      records += values
    } else {
      try {
        writeRecord(file, values)
        recordCount += 1
      } catch {
        case ex: IOException => throw new IOException("Error occured while writing record. " + ex.getMessage)
      }
    }
  }

  /**
   * Writes the set data to the OutputStream.
   */
  @throws(classOf[IOException])
  def write(out: OutputStream) {
    try {
      if (file == null) {
        val outStream = new DataOutputStream(out)

        header.numberOfRecords = records.size
        header.write(outStream)

        /* Now write all the records */
        val t_recCount = records.size
        var i = 0
        while (i < t_recCount) {
          val t_values = records(i)
          writeRecord(outStream, t_values)
          i += 1
        }

        outStream.write(END_OF_DATA)
        outStream.flush
        
      } else {

        /* everything is written already. just update the header for record count and the END_OF_DATA mark */
        header.numberOfRecords = recordCount
        file.seek(0)
        header.write(this.file)
        file.seek(file.length)
        file.writeByte(END_OF_DATA)
        file.close
      }

    } catch {case ex: IOException => throw new IOException(ex.getMessage)}
  }

  @throws(classOf[IOException])
  def write {
    write(null)
  }

  @throws(classOf[IOException])
  private def writeRecord(dataOutput: DataOutput, values: Array[_]) {
    dataOutput.write(' '.toByte)
    var i = 0
    while (i < header.fields.length) {
      header.fields(i).dataType match {
        case 'C' =>
          if (values(i) != null) {
            val str_value = values(i).toString
            dataOutput.write(Utils.textPadding(str_value, charsetName, header.fields(i).length))
          } else {
            dataOutput.write(Utils.textPadding("", charsetName, this.header.fields(i).length))
          }

        case 'D' =>
          if (values(i) != null) {
            val calendar = new GregorianCalendar
            calendar.setTime(values(i).asInstanceOf[Date])
            dataOutput.write(String.valueOf(calendar.get(Calendar.YEAR)).getBytes)
            dataOutput.write(Utils.textPadding(String.valueOf(calendar.get(Calendar.MONTH) + 1), charsetName, 2, Utils.ALIGN_RIGHT, '0'.toByte))
            dataOutput.write(Utils.textPadding(String.valueOf(calendar.get(Calendar.DAY_OF_MONTH)), charsetName, 2, Utils.ALIGN_RIGHT, '0'.toByte))
            
          } else {
            dataOutput.write("        ".getBytes)
          }

        case 'F' =>
          if (values(i) != null) {
            dataOutput.write(Utils.doubleFormating(values(i).asInstanceOf[Double], charsetName, header.fields(i).length, header.fields(i).decimalCount))
          } else {
            dataOutput.write(Utils.textPadding(" ", charsetName, header.fields(i).length, Utils.ALIGN_RIGHT))
          }

        case 'N' =>
          if (values(i) != null) {
            dataOutput.write(Utils.doubleFormating(values(i).asInstanceOf[Double], charsetName, header.fields(i).length, header.fields(i).decimalCount))
          } else {
            dataOutput.write(Utils.textPadding(" ", charsetName, this.header.fields(i).length, Utils.ALIGN_RIGHT))
          }

        case 'L' =>
          if (values(i) != null) {
            if (values(i).asInstanceOf[Boolean] == true) {
              dataOutput.write('T'.toByte)
            } else {
              dataOutput.write('F'.toByte)
            }
          } else {
            dataOutput.write('?'.toByte)
          }

        case 'M' =>
          
        case 'T' =>
          if (values(i) != null) {
            val calendar = new GregorianCalendar
            calendar.setTime(values(i).asInstanceOf[Date])
            dataOutput.write(String.valueOf(calendar.get(Calendar.YEAR)).getBytes)
            dataOutput.write(Utils.textPadding(String.valueOf(calendar.get(Calendar.MONTH) + 1), charsetName, 2, Utils.ALIGN_RIGHT, '0'.toByte))
            dataOutput.write(Utils.textPadding(String.valueOf(calendar.get(Calendar.DAY_OF_MONTH)), charsetName, 2, Utils.ALIGN_RIGHT, '0'.toByte))
            dataOutput.write(Utils.textPadding(String.valueOf(calendar.get(Calendar.HOUR_OF_DAY)), charsetName, 2, Utils.ALIGN_RIGHT, '0'.toByte))
            dataOutput.write(Utils.textPadding(String.valueOf(calendar.get(Calendar.MINUTE)), charsetName, 2, Utils.ALIGN_RIGHT, '0'.toByte))
            dataOutput.write(Utils.textPadding(String.valueOf(calendar.get(Calendar.SECOND)), charsetName, 2, Utils.ALIGN_RIGHT, '0'.toByte))
            dataOutput.write(Utils.textPadding(String.valueOf(calendar.get(Calendar.MILLISECOND)), charsetName, 3, Utils.ALIGN_RIGHT, '0'.toByte))
          } else {
            dataOutput.write("                 ".getBytes)
          }

        case 'I' =>
          if (values(i) != null) {
            val int_value = values(i).toString
            dataOutput.write(Utils.textPadding(int_value, charsetName, header.fields(i).length))
          } else {
            dataOutput.write(Utils.textPadding("0", charsetName, this.header.fields(i).length))
          }
        case _ => throw new IOException("Unknown field type " + header.fields(i).dataType)
      }
      
      i += 1
    }	
  }

  def close {
    try {
      file.close
    } catch {case ex: IOException => throw ex}
  }
}

object DBFWriter {
  private val END_OF_DATA = 0x1A
}
