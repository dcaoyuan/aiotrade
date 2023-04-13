package org.aiotrade.lib.math.random

import java.security.GeneralSecurityException
import java.security.Key
import java.util.Random
import java.util.concurrent.locks.ReentrantLock
import javax.crypto.Cipher

/**
 * <p>Non-linear random number generator based on the AES block cipher in counter mode.
 * Uses the seed as a key to encrypt a 128-bit counter using AES(Rijndael).</p>
 * 
 * <p>By default, we only use a 128-bit key for the cipher because any larger key requires
 * the inconvenience of installing the unlimited strength cryptography policy
 * files for the Java platform.  Larger keys may be used (192 or 256 bits) but if the
 * cryptography policy files are not installed, a
 * {@link java.security.GeneralSecurityException} will be thrown.</p>
 *
 * <p><em>NOTE: THIS CLASS IS NOT SERIALIZABLE</em></p>
 * 
 * @author Daniel Dyer
 */
class AESCounterRNG private (val seed: Array[Byte]) extends Random with RepeatableRNG {
  private val cipher = Cipher.getInstance("AES/ECB/NoPadding") // TO DO: This field is not Serializable.
  cipher.init(Cipher.ENCRYPT_MODE, new AESCounterRNG.AESKey(seed))

  private val counter = new Array[Byte](16) // 128-bit counter.
  // Lock to prevent concurrent modification of the RNG's Internal state.
  private val lock = new ReentrantLock()

  private var currentBlock: Array[Byte] = _
  private var index = 0

  def getSeed: Array[Byte] = {
    seed.clone
  }

  private def incrementCounter {
    var i = 0
    var continue = true
    while (i < counter.length && continue) {
      val c = counter(i)
      counter(i) = (c + 1).toByte
      if (counter(i) != 0) { // Check whether we need to loop again to carry the one.
        continue = false
      } else {
        i += 1
      }
    }
  }


  /**
   * Generates a single 128-bit block (16 bytes).
   * @throws GeneralSecurityException If there is a problem with the cipher
   * that generates the random data.
   * @return A 16-byte block of random data.
   */
  @throws(classOf[GeneralSecurityException])
  private def nextBlock: Array[Byte] = {
    incrementCounter
    cipher.doFinal(counter)
  }


  /**
   * {@inheritDoc}
   */
  override
  final protected def next(bits: Int): Int = {
    var result = 0
    try {
      lock.lock
      if (currentBlock == null || currentBlock.length - index < 4) {
        try {
          currentBlock = nextBlock
          index = 0
        } catch {
          case ex: GeneralSecurityException =>
            // Should never happen.  If initialisation succeeds without exceptions
            // we should be able to proceed indefinitely without exceptions.
            throw new IllegalStateException("Failed creating next random block.", ex)
        }
      }
      result = BinaryUtils.convertBytesToInt(currentBlock, index)
      index += 4
    } finally {
      lock.unlock
    }
    result >>> (32 - bits)
  }

}

object AESCounterRNG {
  private val DEFAULT_SEED_SIZE_BYTES = 16
   
  def apply(seed: Array[Byte]): AESCounterRNG = {
    if (seed == null) {
      throw new IllegalArgumentException("AES RNG requires a 128-bit, 192-bit or 256-bit seed.")
    }
    new AESCounterRNG(seed.clone)
  }
  
  /**
   * Seed the RNG using the provided seed generation strategy to create a 128-bit
   * seed.
   * @param seedGenerator The seed generation strategy that will provide
   * the seed value for this RNG.
   * @throws SeedException If there is a problem generating a seed.
   * @throws GeneralSecurityException If there is a problem initialising the AES cipher.
   */
  @throws(classOf[SeedException]) 
  @throws(classOf[GeneralSecurityException])
  def apply(seedGenerator: SeedGenerator): AESCounterRNG = {
    apply(seedGenerator.generateSeed(DEFAULT_SEED_SIZE_BYTES))
  }


  /**
   * Seed the RNG using the default seed generation strategy to create a seed of the
   * specified size.
   * @param seedSizeBytes The number of bytes to use for seed data.  Valid values
   * are 16 (128 bits), 24 (192 bits) and 32 (256 bits).  Any other values will
   * result in an exception from the AES implementation.
   * @throws GeneralSecurityException If there is a problem initialising the AES cipher.
   * @since 1.0.2
   */
  @throws(classOf[GeneralSecurityException])
  def apply(seedSizeBytes: Int): AESCounterRNG = {
    apply(SeedGenerator.generateSeed(seedSizeBytes))
  }

  /**
   * Creates a new RNG and seeds it using 128 bits from the default seeding strategy.
   * @throws GeneralSecurityException If there is a problem initialising the AES cipher.
   */
  @throws(classOf[GeneralSecurityException])
  def apply(): AESCounterRNG = {
    apply(DEFAULT_SEED_SIZE_BYTES)
  }
  
  /**
   * Trivial key implementation for use with AES cipher.
   */
  private final class AESKey(keyData: Array[Byte]) extends Key {
    def getAlgorithm = "AES"
    def getFormat = "RAW"
    def getEncoded: Array[Byte] = keyData
  }
}
