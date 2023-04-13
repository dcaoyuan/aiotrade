package org.aiotrade.lib.math.random

import java.nio.charset.Charset
import java.util.Random

final class RandomWrapper(fixedSeed: Long) extends Random {
  private val STANDARD_SEED = "Mahout=Hadoop+ML".getBytes(Charset.forName("US-ASCII"))
  private val SEED_GENERATOR = FastRandomSeedGenerator

  private var random: Random = buildRandom()

  def this() = {
    this(Long.MinValue)
  }

  private def buildRandom(): Random = {
    if (fixedSeed == Long.MinValue) {
      if (RandomWrapper.testSeed) {
        MersenneTwisterRNG(STANDARD_SEED)
      } else {
        // Force use of standard generator, and disallow use of those based on /dev/random since
        // it causes hangs on Ubuntu
        try {
          MersenneTwisterRNG(SEED_GENERATOR)
        } catch {
          case ex: SeedException =>
            // Can't happen
            throw new IllegalStateException(ex)
        }
      }
    } else {
      MersenneTwisterRNG(RandomUtils.longSeedtoBytes(fixedSeed))
    }
  }

  def getRandom: Random = {
    random
  }

  def reset {
    random = buildRandom
  }

  def getSeed: Long = {
    RandomUtils.seedBytesToLong(random.asInstanceOf[RepeatableRNG].getSeed)
  }

  override
  def setSeed(seed: Long) {
    // Since this will be called by the java.util.Random() constructor before we construct
    // the delegate... and because we don't actually care about the result of this for our
    // purpose:
    random = MersenneTwisterRNG(RandomUtils.longSeedtoBytes(seed))
  }

  override
  protected def next(bits: Int): Int = {
    // Ugh, can't delegate this method -- it's protected
    // Callers can't use it and other methods are delegated, so shouldn't matter
    throw new UnsupportedOperationException
  }

  override def nextBytes(bytes: Array[Byte]): Unit = random.nextBytes(bytes)
  override def nextInt: Int = random.nextInt
  override def nextInt(n: Int): Int = random.nextInt(n)
  override def nextLong: Long = random.nextLong
  override def nextBoolean: Boolean = random.nextBoolean
  override def nextFloat: Float = random.nextFloat
  override def nextDouble: Double = random.nextDouble
  override def nextGaussian: Double = random.nextGaussian
}

object RandomWrapper {
  private var testSeed: Boolean = _

  protected[random] def useTestSeed {
    testSeed = true
  }
}