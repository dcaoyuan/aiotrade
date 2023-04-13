package org.aiotrade.lib.math.random

import java.security.SecureRandom

/**
 * <p>{@link SeedGenerator} implementation that uses Java's bundled
 * {@link SecureRandom} RNG to generate random seed data.</p>
 *
 * <p>The advantage of using SecureRandom for seeding but not as the
 * primary RNG is that we can use it to seed RNGs that are much faster
 * than SecureRandom.</p>
 *
 * <p>This is the only seeding strategy that is guaranteed to work on all
 * platforms and therefore is provided as a fall-back option should
 * none of the other provided {@link SeedGenerator} implementations be
 * useable.</p>
 * @author Daniel Dyer
 */
object SecureRandomSeedGenerator extends SeedGenerator {
  private val SOURCE = new SecureRandom()
  
  @throws(classOf[SeedException])
  def generateSeed(length: Int): Array[Byte] = {
    SecureRandomSeedGenerator.SOURCE.generateSeed(length)
  }

  override
  def toString = {
    "java.security.SecureRandom"
  }
}
