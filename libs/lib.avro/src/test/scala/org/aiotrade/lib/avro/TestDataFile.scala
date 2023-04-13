package org.aiotrade.lib.avro

import java.io.File
import java.io.IOException
import java.util.Random
import java.util.logging.Logger
import org.apache.avro.Schema
import org.apache.avro.file.CodecFactory
import org.apache.avro.file.DataFileReader
import org.apache.avro.file.DataFileWriter
import org.apache.avro.io.DatumReader
import scala.collection.JavaConversions._

object TestDataFile {
  private val log = Logger.getLogger(this.getClass.getName)

  private val COUNT = Integer.parseInt(System.getProperty("test.count", "500"))
  private val VALIDATE = System.getProperty("test.validate", "true") != "false"
  private val USER_HOME = System.getProperty("user.home")
  private val DIR = new File(System.getProperty("test.dir", USER_HOME + "/tmp"))
  private val DATAFILE_DIR = new File(System.getProperty("test.dir", USER_HOME+ "/tmp"))
  // We can set a fixed seed to get RandomData always output same datums.
  private val SEED = 1L //System.currentTimeMillis
  private val syncInterval = 1024 * 100


  private val SCHEMA_JSON = """
  {"type": "record", "name": "Test", "fields": [
    {"name":"stringField", "type":"string"},
    {"name":"longField", "type":"long"}
   ]}
  """
  private val SCHEMA = Schema.parse(SCHEMA_JSON)

  private var codec: CodecFactory = _

  def main(args: Array[String]) {
    testGenericWrite
    //testGenericRead
  }
  
  def testRead(args: Array[String]) {
    val input = new File(args(0))
    log.info("Running with codec: " + codec)

    val projection: Schema = if (args.length > 1) Schema.parse(new File(args(1))) else null
    readFile(input, GenericDatumReader[Object](null, projection))

    val start = System.currentTimeMillis
    for (i <- 0 until 4) {
      readFile(input, GenericDatumReader[Object](null, projection))
    }
    println("Time: " + (System.currentTimeMillis - start))
  }

  def codecs = {
    val r = new java.util.ArrayList[Array[Object]]()
    r.add(Array( null ))
    r.add(Array( CodecFactory.deflateCodec(0) ))
    r.add(Array( CodecFactory.deflateCodec(1) ))
    r.add(Array( CodecFactory.deflateCodec(9) ))
    r.add(Array( CodecFactory.nullCodec() ))
    r
  }

  private def makeFile= new File(DIR, "test-" + codec + ".avro")

  @throws(classOf[IOException])
  def testGenericWrite {
    val file = makeFile
    val writer = new DataFileWriter[Object](GenericDatumWriter[Object]()).setSyncInterval(syncInterval)
    if (codec != null) {
      writer.setCodec(codec)
    }
    writer.create(SCHEMA, file)
    try {
      var count = 0
      for (datum <- new RandomData(SCHEMA, COUNT, SEED)) {
        writer.append(datum)
        count += 1
      }
      println("Count: " + count + " with syncInterval=" + syncInterval)
    } finally {
      writer.close
      println("File: " + file.getCanonicalPath + ", size=" + file.length)
    }
  }

  @throws(classOf[IOException])
  def testGenericRead {
    val reader = new DataFileReader[Object](makeFile, GenericDatumReader[Object]())
    try {
      var datum: Object = null
      if (VALIDATE) {
        for (expected <- new RandomData(SCHEMA, COUNT, SEED)) {
          datum = reader.next(datum)
          if (expected != datum) {
            println("Error: " + expected + " != " + datum)
          }
        }
      } else {
        for (i <- 0 until COUNT) {
          datum = reader.next(datum)
        }
      }
    } finally {
      reader.close
    }
  }

  @throws(classOf[IOException])
  def testSplits {
    val file = makeFile
    val reader = new DataFileReader[Object](file, GenericDatumReader[Object]())
    val rand = new Random(SEED)
    try {
      val splits = 10                         // number of splits
      val length = file.length.toInt          // length of file
      var end = length                        // end of split
      var remaining = end                     // bytes remaining
      var count = 0                           // count of entries
      while (remaining > 0) {
        val start = math.max(0, end - rand.nextInt(2*length/splits))
        reader.sync(start)                    // count entries in split
        while (!reader.pastSync(end)) {
          reader.next
          count += 1
        }
        remaining -= end-start
        end = start
      }
      //assertEquals(COUNT, count);
    } finally {
      reader.close
    }
  }

  @throws(classOf[IOException])
  def testSyncDiscovery {
    val file = makeFile
    val reader = new DataFileReader[Object](file, GenericDatumReader[Object]())
    try {
      // discover the sync points
      val syncs = new java.util.ArrayList[Long]()
      var previousSync = -1L
      while (reader.hasNext) {
        if (reader.previousSync() != previousSync) {
          previousSync = reader.previousSync
          syncs.add(previousSync)
        }
        reader.next
      }
      // confirm that the first point is the one reached by sync(0)
      reader.sync(0)
      //assertEquals((long)reader.previousSync(), (long)syncs.get(0));
      // and confirm that all points are reachable
      for (sync <- syncs) {
        reader.seek(sync)
        //assertNotNull(reader.next());
      }
    } finally {
      reader.close
    }
  }

  @throws(classOf[IOException])
  def testGenericAppend {
    val file = makeFile
    val start = file.length
    val writer = new DataFileWriter[Object](GenericDatumWriter[Object]()).appendTo(file);
    try {
      for (datum <- new RandomData(SCHEMA, COUNT, SEED+1)) {
        writer.append(datum)
      }
    } finally {
      writer.close
    }
    val reader = new DataFileReader[Object](file, GenericDatumReader[Object]());
    try {
      reader.seek(start)
      var datum: Object = null
      if (VALIDATE) {
        for (expected <- new RandomData(SCHEMA, COUNT, SEED+1)) {
          datum = reader.next(datum);
          //assertEquals(expected, datum);
        }
      } else {
        for (i <- 0 until COUNT) {
          datum = reader.next(datum)
        }
      }
    } finally {
      reader.close
    }
  }

  @throws(classOf[IOException])
  protected def readFile(f: File, datumReader: DatumReader[Object]) {
    System.out.println("Reading "+ f.getName)
    val reader = new DataFileReader[Object](f, datumReader).iterator
    for (datum <- reader) {
      datum != null
      //assertNotNull(datum);
    }
  }

  class InteropTest {

    @throws(classOf[IOException])
    def testGeneratedGeneric {
      System.out.println("Reading with generic:")
      readFiles(GenericDatumReader[Object]())
    }

    @throws(classOf[IOException])
    def testGeneratedSpecific {
      System.out.println("Reading with specific:")
      readFiles(SpecificDatumReader[Object]())
    }

    // Can't use same Interop.java as specific for reflect, since its stringField
    // has type Utf8, which reflect would try to assign a String to.  We could
    // fix this by defining a reflect-specific version of Interop.java, but we'd
    // need to put it on a different classpath than the specific one.

    // @Test
    //   public void testGeneratedReflect() throws IOException {
    //     readFiles(new ReflectDatumReader(Interop.class));
    //   }

    @throws(classOf[IOException])
    private def readFiles(datumReader: DatumReader[Object]) {
      for (f <- DATAFILE_DIR.listFiles)
        readFile(f, datumReader)
    }
  }
}
