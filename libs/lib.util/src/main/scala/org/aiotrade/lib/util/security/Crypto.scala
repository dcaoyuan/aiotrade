package org.aiotrade.lib.util.security

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

object Crypto {

  private val AES = "AES"

  // KeyGenerator objects are reusable
  private val keygen = KeyGenerator.getInstance(AES)
  private val random = new SecureRandom

  def generateKey: SecretKey = {
    keygen.init(random)
    val key = keygen.generateKey
    val raw = key.getEncoded
    new SecretKeySpec(raw, AES)
  }

  def keyToBytes(key: SecretKey): Array[Byte] = {
    val os = new ByteArrayOutputStream
    val out = new ObjectOutputStream(os)
    out.writeObject(key)
    out.close
    os.toByteArray
  }

  def keyFromBytes(bytes: Array[Byte]): SecretKey = {
    val in = new ObjectInputStream(new ByteArrayInputStream(bytes))
    in.readObject.asInstanceOf[SecretKey]
  }

  def encrypt(content: String, key: SecretKey): Array[Byte] = {
    encrypt(content.getBytes("UTF-8"), key)
  }

  def encrypt(bytes: Array[Byte], key: SecretKey): Array[Byte] = {
    val cipher = Cipher.getInstance(AES)
    cipher.init(Cipher.ENCRYPT_MODE, key)
    cipher.doFinal(bytes)
  }

  def decrypt(bytes: Array[Byte], key: SecretKey): Array[Byte] = {
    val cipher = Cipher.getInstance(AES)
    cipher.init(Cipher.DECRYPT_MODE, key)
    cipher.doFinal(bytes)
  }

  // --- simple test
  def main(args: Array[String]) {
    keygen.init(random)
    val keySpec = generateKey
    val en = encrypt("test", keySpec)
    println(en.mkString("(", ",", ")"))
    val de = decrypt(en, keySpec)
    println(new String(de, "UTF-8"))
  }

}
