package org.aiotrade.lib.math.random

import java.io.BufferedOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Random

/**
 * Utility to generate an input file for the
 * <a href="http://stat.fsu.edu/pub/diehard/" target="_top">DIEHARD</a> suite of statistical
 * tests for random number generators.
 * @author Daniel Dyer
 */
object DiehardInputGenerator {
  // How many 32-bit values should be written to the output file.
  private val INT_COUNT = 3000000

  /**
   * @param args The first argument is the class name of the RNG, the second
   * is the file to use for output.
   * @throws Exception If there are problems setting up the RNG or writing to
   * the output file.
   */
  @throws(classOf[Exception])
  def main(args: Array[String]) {
    if (args.length != 2) {
      System.out.println("Expected arguments:")
      System.out.println("\t<Fully-qualified RNG class name> <Output file>")
      System.exit(1)
    }
    val rngClass = Class.forName(args(0)).asInstanceOf[Class[_ <: Random]]
    val outputFile = new File(args(1))
    generateOutputFile(rngClass.newInstance, outputFile)
  }


  /**
   * Generates a file of random data in a format suitable for the DIEHARD test.
   * DIEHARD requires 3 million 32-bit integers.
   * @param rng The random number generator to use to generate the data.
   * @param outputFile The file that the random data is written to.
   * @throws IOException If there is a problem writing to the file.
   */
  @throws(classOf[IOException])
  def generateOutputFile(rng: Random, outputFile: File) {
    var out: DataOutputStream = null
    try {
      out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outputFile)))
      var i = 0
      while (i < INT_COUNT) {
        out.writeInt(rng.nextInt)
        i += 1
      }
      out.flush
    } finally {
      if (out != null) {
        out.close
      }
    }
  }
}
