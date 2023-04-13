package org.aiotrade.lib.dbfdriver

import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.io.RandomAccessFile
import java.io.UnsupportedEncodingException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.Charset
import java.util.Calendar


@throws(classOf[IOException])
@throws(classOf[UnsupportedEncodingException])
class DBFReader private (input: Either[FileChannel, InputStream], charsetName: String) {
  import DBFReader._
  val charset = Charset.forName(if (charsetName == null) defaultCharsetName else charsetName)
  var isClosed = false

  private var bBuf: ByteBuffer = _
  var header: DBFHeader = _

  private val cal = Calendar.getInstance

  load

  /** Can be reloaded */
  def load {
    input match {
      case Left(x) => load(x)
      case Right(x) => load(x)
    }
  }

  private def load(fileChannel: FileChannel) {
    if (isClosed) throw new IOException("This reader has closed")

    //bBuf = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size)
    bBuf = ByteBuffer.allocate(fileChannel.size.toInt)
    fileChannel.position(0)
    fileChannel.read(bBuf)
    bBuf.rewind
    
    header = DBFHeader.read(bBuf)
  }

  private def load(is: InputStream) {
    if (isClosed) throw new IOException("This reader has closed")
    
    val out = new ByteArrayOutputStream
    val buf = new Array[Byte](4096)
    val bis = new BufferedInputStream(is)
    var len = -1
    while ({len = bis.read(buf); len != -1}) {
      out.write(buf, 0, len)
    }
    val bytes = out.toByteArray
    bBuf = ByteBuffer.wrap(bytes)
    
    header = DBFHeader.read(bBuf)
  }


  // it might be required to leap to the start of records at times
  val dataStartIndex = header.headerLength - (32 + (32 * header.fields.length)) - 1
  if (dataStartIndex > 0) {
    bBuf.position(bBuf.position + dataStartIndex)
    //is.skip(dataStartIndex)
  }

  override def toString = {
    val sb = new StringBuilder
    sb.append(header.year).append("/").append(header.month).append("/").append(header.day).append("\n")
    sb.append("Total records: ").append(header.numberOfRecords)
    sb.append("\nHeader length: ").append(header.headerLength)

    header.fields map (_.name) mkString("/n")

    sb.toString
  }

  /**
   * Returns the number of records in the DBF.
   */
  def recordCount = header.numberOfRecords

  /**
   * Returns the asked Field. In case of an invalid index,
   * it returns a ArrayIndexOutofboundsException.
   *
   * @param index. Index of the field. Index of the first field is zero.
   */
  @throws(classOf[IOException])
  def getField(idx: Int): DBFField = {
    if (isClosed) {
      throw new IOException("Source is not open")
    }
    header.fields(idx)
  }

  /**
   * Returns the number of field in the DBF.
   */
  @throws(classOf[IOException])
  def fieldCount: Int = {
    if (isClosed) {
      throw new IOException("Source is not open")
    }
    if (header.fields != null) {
      header.fields.length
    } else -1
  }

  def hasNext: Boolean = bBuf.position < bBuf.capacity

  /**
   * Reads the returns the next row in the DBF stream.
   * @returns The next row as an Object array. Types of the elements
   * these arrays follow the convention mentioned in the class description.
   */
  @throws(classOf[IOException])
  def nextRecord: Array[Any] = {
    if (isClosed) {
      throw new IOException("Source is not open")
    }

    val values = new Array[Any](header.fields.length)
    try {
      bBuf.get //Skip the Record deleted flag
      
//      var isDeleted = false
//      do {
//        if (isDeleted) {
//          try{
//            in.position(in.position + header.recordLength - 1)
//          }catch{case ex: Exception => return null}
//          //is.skip(header.recordLength - 1)
//        }
//
//        if(in.position >= in.capacity)
//          return null
//
//        val b = in.get
//        if (b == END_OF_DATA) {
//          return null
//        }
//
//        isDeleted = (b == '*')
//      } while (isDeleted)

      val bytes2 = new Array[Byte](2)
      val bytes3 = new Array[Byte](3)
      val bytes4 = new Array[Byte](4)
      var i = -1
      while ({i += 1; i < header.fields.length}) {
        val obj = header.fields(i).dataType match {
          case 'C' =>
            try {
              val bytes = new Array[Byte](header.fields(i).length)
              bBuf.get(bytes)
              new String(bytes, charsetName)
            } catch {
              case _: Throwable => ""
            }
          case 'D' =>
            try {
              cal.clear()
              
              bBuf.get(bytes4)
              cal.set(Calendar.YEAR, parseInt(bytes4))
              
              bBuf.get(bytes2)
              cal.set(Calendar.MONTH, parseInt(bytes2) - 1)
              
              bBuf.get(bytes2)
              cal.set(Calendar.DAY_OF_MONTH, parseInt(bytes2))

              cal.getTime
            } catch {
              case _: Throwable => null // this field may be empty or may have improper value set
            } 

          case 'F' =>
            try {
              val bytes = new Array[Byte](header.fields(i).length)
              bBuf.get(bytes)
              
              parseFloat(bytes)
            } catch {
              case ex: NumberFormatException => 0.0F // throw new IOException("Failed to parse Float: " + ex.getMessage)
              case _: Throwable => 0.0F
            }

          case 'N' =>
            try {
              val bytes = new Array[Byte](header.fields(i).length)
              bBuf.get(bytes)
              
              parseDouble(bytes)
            } catch {
              case ex: NumberFormatException => 0.0 // throw new IOException("Failed to parse Number: " + ex.getMessage)
              case _: Throwable => 0.0
            }

          case 'L' =>
            bBuf.get match {
              case 'Y' | 'y' | 'T' | 't' => true
              case _ => false
            }
            
          case 'M' => "null" // TODO Later

          case 'T' =>
            try {
              cal.clear()
              
              bBuf.get(bytes4)
              cal.set(Calendar.YEAR, parseInt(bytes4))

              val bytes = new Array[Byte](2) // month
              bBuf.get(bytes2)
              cal.set(Calendar.MONTH, parseInt(bytes2) - 1)

              bBuf.get(bytes2) // day
              cal.set(Calendar.DAY_OF_MONTH, parseInt(bytes2))

              bBuf.get(bytes2) // hour
              cal.set(Calendar.HOUR_OF_DAY, parseInt(bytes2))
              
              bBuf.get(bytes2) // min
              cal.set(Calendar.MINUTE, parseInt(bytes2))

              bBuf.get(bytes2) // sec
              cal.set(Calendar.SECOND, parseInt(bytes2))
              
              bBuf.get(bytes3)
              cal.set(Calendar.MILLISECOND, parseInt(bytes3))
              
              cal.getTime
            } catch {
              case _: Throwable => null // this field may be empty or may have improper value set
            }
          case 'I' =>
            try{
              val bytes = new Array[Byte](header.fields(i).length)
              bBuf.get(bytes)
              parseInt(bytes)
            } catch{
              case _: Throwable => 0
            }
          case _ => "null"
        }
        
        values(i) = obj
      }
    } catch {
      case ex: EOFException => return null
      case ex: IOException => throw new IOException(ex.getMessage)
    }

    values
  }
  
  def close {
    isClosed = true
    try {
      input match {
        case Left(x) => x.close
        case Right(x) => x.close
      }
    } catch {
      case ex: IOException => throw ex
    }
  }
  
  // --- helper
  
  /**
   * filter space char ' ' and convert to a char array, then a string.
   * 
   * new String(chars: Char[Array]) is the direct and fastest way for ascii 
   * encoded bytes
   */
  private def asciiNumberBytesToString(bytes: Array[Byte]): String = {
    val chars = new Array[Char](bytes.length)
    var i = -1
    var j = -1
    while ({i += 1; i < bytes.length}) {
      val byte = bytes(i)
      if (byte == '?') return null // invalid char
      
      if (byte != ' ') {
        j += 1
        chars(j) = byte.asInstanceOf[Char]
      }
    }
    if (j >= 0) new String(chars, 0, j + 1) else null
  }

  @throws(classOf[NumberFormatException])
  final def parseInt(bytes: Array[Byte]): Int = {
    val str = asciiNumberBytesToString(bytes)
    if (str ne null) java.lang.Integer.parseInt(str) else 0
  }
  
  @throws(classOf[NumberFormatException])
  final def parseFloat(bytes: Array[Byte]): Float = {
    val str = asciiNumberBytesToString(bytes)
    if (str ne null) java.lang.Float.parseFloat(str) else 0.0F
  }

  @throws(classOf[NumberFormatException])
  final def parseDouble(bytes: Array[Byte]): Double = {
    val str = asciiNumberBytesToString(bytes)
    if (str ne null) java.lang.Double.parseDouble(str) else 0.0
  }
  
}

object DBFReader {
  private val defaultCharsetName = "8859_1"
  
  private val END_OF_DATA = 0x1A

  @throws(classOf[UnsupportedEncodingException])
  @throws(classOf[IOException])
  def apply(is: InputStream, charsetName: String): DBFReader = is match {
    case x: FileInputStream => createFileChannelDBFReader(x.getChannel, charsetName)
    case _ => new DBFReader(Right(is), charsetName)
  }

  @throws(classOf[UnsupportedEncodingException])
  @throws(classOf[IOException])
  def apply(file: File, charsetName: String): DBFReader = {
    createFileChannelDBFReader(new FileInputStream(file).getChannel, charsetName)
  }

  @throws(classOf[UnsupportedEncodingException])
  @throws(classOf[IOException])
  def apply(fileName: String, charsetName: String): DBFReader = {
    createFileChannelDBFReader(new RandomAccessFile(fileName, "r").getChannel, charsetName)
  }
  
  @throws(classOf[UnsupportedEncodingException])
  @throws(classOf[IOException])
  def apply(fileChannel: FileChannel, charsetName: String): DBFReader = {
    createFileChannelDBFReader(fileChannel, charsetName)
  }
  
  @throws(classOf[UnsupportedEncodingException])
  @throws(classOf[IOException])
  private def createFileChannelDBFReader(fileChannel: FileChannel, charsetName: String): DBFReader = {
    try {
      new DBFReader(Left(fileChannel), charsetName)
    } catch {
      case ex: Throwable => 
        if (fileChannel != null) {
          try {
            fileChannel.close
          } catch {
            case _: Throwable =>
          }
        }

        throw ex
    }
  }
}
