package org.aiotrade.lib.math.random

/**
 * Strategy interface for seeding random number generators.
 * @author Daniel Dyer
 */
trait SeedGenerator {
  /**
   * Generate a seed value for a random number generator.
   * @param length The length of the seed to generate (in bytes).
   * @return A byte array containing the seed data.
   * @throws SeedException If a seed cannot be generated for any reason.
   */
  @throws(classOf[SeedException])
  def generateSeed(length: Int): Array[Byte]
}

object SeedGenerator extends SeedGenerator {
  private val DEBUG_PROPERTY = "org.aiotrade.lib.math.random.debug"

  /** Delegate generators. */
  private val GENERATORS = List[SeedGenerator](
    DevRandomSeedGenerator,
    RandomDotOrgSeedGenerator,
    SecureRandomSeedGenerator
  )

  /**
   * Generates a seed by trying each of the available strategies in
   * turn until one succeeds.  Tries the most suitable strategy first
   * and eventually degrades to the least suitable (but guaranteed to
   * work) strategy.
   * @param length The length (in bytes) of the seed.
   * @return A random seed of the requested length.
   */
  def generateSeed(length: Int): Array[Byte] = {
    for (generator <- GENERATORS) {
      try {
        val seed = generator.generateSeed(length)
        try {
          val debug = System.getProperty(DEBUG_PROPERTY, "false").equals("true")
          if (debug) {
            val seedString = BinaryUtils.convertBytesToHexString(seed)
            System.out.println(seed.length + " bytes of seed data acquired from " + generator + ":")
            System.out.println("  " + seedString)
          }
        } catch {
          case ex: SecurityException =>
        }
        seed
      } catch {
        case ex: SeedException =>
      }
    }
    // This shouldn't happen as at least one the generators should be
    // able to generate a seed.
    throw new IllegalStateException("All available seed generation strategies failed.")
  }
}


trait SeedException extends Exception
object SeedException {
  def apply(message: String, cause: Throwable) = new Exception(message, cause) with SeedException
  def apply(message: String) = new Exception(message) with SeedException
}