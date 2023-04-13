/*                     __                                               *\
 **     ________ ___   / /  ___     Scala API                            **
 **    / __/ __// _ | / /  / _ |    (c) 2003-2010, LAMP/EPFL             **
 **  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
 ** /____/\___/_/ |_/____/_/ | |                                         **
 **                          |/                                          **
 \*                                                                      */

// $Id: ArrayList.scala 19223 2009-10-22 10:43:02Z malayeri $


package org.aiotrade.lib.collection

import scala.collection.CustomParallelizable
import scala.collection.generic._
import scala.collection.mutable.Buffer
import scala.collection.mutable.BufferLike
import scala.collection.mutable.Builder
import scala.collection.mutable.IndexedSeq
import scala.collection.mutable.IndexedSeqOptimized
import scala.collection.mutable.Seq
import scala.collection.mutable.WrappedArray
import scala.collection.parallel.mutable.ParArray
import scala.reflect.ClassTag


/** An implementation of the <code>Buffer</code> class using an array to
 *  represent the assembled sequence internally. Append, update and random
 *  access take constant time (amortized time). Prepends and removes are
 *  linear in the buffer size.
 *
 *  serialver -classpath ~/myapps/scala/lib/scala-library.jar:./ org.aiotrade.lib.collection.ArrayList
 *  
 *  @author  Matthias Zenger
 *  @author  Martin Odersky
 *  @version 2.8
 *  @since   1
 *  
 *  @author  Caoyuan Deng
 *  @note Don't add @specialized in front of A, which will generate a shadow array and waste memory
 */

@SerialVersionUID(1529165946227428979L)
final class ArrayList[A](  
  _initialSize: Int, _elementClass: Option[Class[A]] = None
)(implicit _m: ClassTag[A]) extends AbstractArrayList[A](_initialSize, _elementClass)(_m) 
                               with GenericTraversableTemplate[A, ArrayList]
                               with BufferLike[A, ArrayList[A]]
                               with IndexedSeqOptimized[A, ArrayList[A]]
                               with Builder[A, ArrayList[A]] {

  override 
  def companion: GenericCompanion[ArrayList] = ArrayList

  def this()(implicit m: ClassTag[A]) = this(16)
  
  def result: ArrayList[A] = this

  override 
  def reverse: ArrayList[A] = {
    val reversed = new ArrayList[A](size)
    var i = 0
    while (i < size) {
      reversed(i) = apply(size - 1 - i)
      i += 1
    }
    reversed
  }

  override 
  def partition(p: A => Boolean): (ArrayList[A], ArrayList[A]) = {
    val l, r = new ArrayList[A]
    for (x <- this) (if (p(x)) l else r) += x
    (l, r)
  }

  /** Return a clone of this buffer.
   *
   *  @return an <code>ArrayList</code> with the same elements.
   */
  override 
  def clone(): ArrayList[A] = new ArrayList[A](size) ++= this
  
  def sliceToArrayList(start: Int, len: Int): ArrayList[A] = {
    val res = new ArrayList(len)
    scala.compat.Platform.arraycopy(array, start, res.array, 0, len)
    res
  }
}


/** Factory object for <code>ArrayBuffer</code> class.
 *
 *  @author  Martin Odersky
 *  @version 2.8
 *  @since   2.8
 */
object ArrayList extends SeqFactory[ArrayList] {
  implicit def canBuildFrom[A]: CanBuildFrom[Coll, A, ArrayList[A]] = new GenericCanBuildFrom[A]
  /**
   * we implement newBuilder for extending SeqFactory only. Since it's with no ClassTag arg,
   * we can only create a ArrayList[Any] instead of ArrayList[A], but we'll define another
   * apply method to create ArrayList[A]
   */
  def newBuilder[A]: Builder[A, ArrayList[A]] = new ArrayList[Any]().asInstanceOf[ArrayList[A]]
  def apply[A: ClassTag]() = new ArrayList[A]
}

/** Explicit instantiation of the `Buffer` trait to reduce class file size in subclasses. */
private[collection] abstract class collectionAbstractTraversable[+A] extends scala.collection.Traversable[A]
private[collection] abstract class collectionAbstractIterable[+A] extends collectionAbstractTraversable[A] with scala.collection.Iterable[A]
private[collection] abstract class collectionAbstractSeq[+A] extends collectionAbstractIterable[A] with scala.collection.Seq[A]
private[collection] abstract class AbstractSeq[A] extends collectionAbstractSeq[A] with Seq[A]
private[collection] abstract class AbstractBuffer[A] extends AbstractSeq[A] with Buffer[A]

abstract class AbstractArrayList[A](  
  override protected val initialSize: Int, 
  protected val elementClass: Option[Class[A]]
)(protected implicit val m: ClassTag[A]) extends AbstractBuffer[A]
                                            with Buffer[A]
                                            with GenericTraversableTemplate[A, AbstractArrayList]
                                            with BufferLike[A, AbstractArrayList[A]]
                                            with IndexedSeqOptimized[A, AbstractArrayList[A]]
                                            with Builder[A, AbstractArrayList[A]]
                                            with ResizableArray[A]
                                            with CustomParallelizable[A, ParArray[A]] 
                                            with Serializable {
  
  override 
  def companion: GenericCompanion[AbstractArrayList] = AbstractArrayList
  
  def clear() {
    reduceToSize(0)
  }
  
  override 
  def sizeHint(len: Int) {
    if (len > size && len >= 1) {
      val newarray = makeArray(len)
      scala.compat.Platform.arraycopy(array, 0, newarray, 0, size0)
      array = newarray
    }
  }
  
  override 
  def par = ParArray.handoff[A](array, size)  
  
  /** Appends a single element to this buffer and returns
   *  the identity of the buffer. It takes constant time.
   *
   *  @param elem  the element to append.
   */
  def +(elem: A): this.type =  {
    ensureSize(size0 + 1)
    array(size0) = elem
    size0 += 1
    this
  }

  /** Appends a single element to this buffer and returns
   *  the identity of the buffer. It takes constant time.
   *
   *  @param elem  the element to append.
   */
  def +=(elem: A): this.type = {
    this.+(elem)
  }

  /** Appends a number of elements provided by an iterable object
   *  via its <code>iterator</code> method. The identity of the
   *  buffer is returned.
   *
   *  @param xs  the itertable object.
   */
  def ++(xs: TraversableOnce[A]): this.type =  {
    val len = xs match {
      case xs: IndexedSeq[A] => xs.length
      case _ => xs.size
    }
    ensureSize(size0 + len)
    xs match {
      // according to the way arrays work in 2.8: An implicit conversion takes 
      // Java arrays to `WrappedArray` if you need a `Traversable` instance.  
      // @see https://lampsvn.epfl.ch/trac/scala/ticket/2564
      case xs: WrappedArray[A] =>
        scala.compat.Platform.arraycopy(xs.array, 0, array, size0, len)
        size0 += len
        this
      case xs: IndexedSeq[A] =>
        xs.copyToArray(array, size0, len)
        size0 += len
        this
      case _ =>
        super.++=(xs)
    }
  }
  
  /** Appends a number of elements provided by an iterable object
   *  via its <code>iterator</code> method. The identity of the
   *  buffer is returned.
   *
   *  @param xs  the itertable object.
   *  @return    the updated buffer.
   */
  override 
  def ++=(xs: TraversableOnce[A]): this.type = {
    this.++(xs)
  }
  
  /** Prepends a single element to this buffer and return
   *  the identity of the buffer. It takes time linear in 
   *  the buffer size.
   *
   *  @param elem  the element to append.
   */
  def +:(elem: A): this.type = {
    ensureSize(size0 + 1)
    copy(0, 1, size0)
    array(0) = elem
    size0 += 1
    this
  }
   
  /** Prepends a single element to this buffer and return
   *  the identity of the buffer. It takes time linear in 
   *  the buffer size.
   *
   *  @param elem  the element to append.
   *  @return      the updated buffer. 
   */
  def +=:(elem: A): this.type = {
    this.+:(elem)
  }
  
  /** Prepends a number of elements provided by an iterable object
   *  via its <code>iterator</code> method. The identity of the
   *  buffer is returned.
   *
   *  @param xs  the iterable object.
   */
  def ++:(xs: TraversableOnce[A]): this.type = {
    insertAll(0, xs.toTraversable)
    this
  }

  /** Prepends a number of elements provided by an iterable object
   *  via its <code>iterator</code> method. The identity of the
   *  buffer is returned.
   *
   *  @param xs  the iterable object.
   *  @return    the updated buffer.
   */
  override 
  def ++=:(xs: TraversableOnce[A]): this.type = {
    this.++:(xs)
  }
  
  /** Inserts new elements at a given index into this buffer.
   *
   *  @param n      the index where new elements are inserted.
   *  @param elems  the traversable collection containing the elements to insert.
   *  @throws   IndexOutOfBoundsException if the index `n` is not in the valid range
   *            `0 <= n <= length`.
   *  
   *  override scala.collection.mutable.BufferLike.insert
   */
  @deprecated("Use insertAll(n: Int, elems: Traversable[A]) or insertOne(n: Int, elem: A), this method may cause ArrayStoreException.", "Since Scala 2.10.0")
  override 
  def insert(n: Int, elems: A*) {
    throw new UnsupportedOperationException("Use insertAll(n: Int, elems: Traversable[A]) or insertOne(n: Int, elem: A), this method may cause ArrayStoreException.")
  }
  
  def insertOne(n: Int, elem: A) {
    if ((n < 0) || (n > size0)) throw new IndexOutOfBoundsException(n.toString)
    ensureSize(size0 + 1)
    copy(n, n + 1, size0 - n)
    array(n) = elem
    size0 += 1
  }

  /** Inserts new elements at the index <code>n</code>. Opposed to method
   *  <code>update</code>, this method will not replace an element with a
   *  one. Instead, it will insert a new element at index <code>n</code>.
   *
   *  @param n     the index where a new element will be inserted.
   *  @param iter  the iterable object providing all elements to insert.
   *  @throws Predef.IndexOutOfBoundsException if <code>n</code> is out of bounds.
   */
  def insertAll(n: Int, elems: scala.collection.Traversable[A]) {
    if ((n < 0) || (n > size0)) throw new IndexOutOfBoundsException(n.toString)
    val len = elems match {
      case xs: IndexedSeq[A] => xs.length
      case _ => elems.size
    }
    ensureSize(size0 + len)
    copy(n, n + len, size0 - n)
    elems match {
      // according to the way arrays work in 2.8: An implicit conversion takes 
      // Java arrays to `WrappedArray` if you need a `Traversable` instance.  
      // @see https://lampsvn.epfl.ch/trac/scala/ticket/2564
      case xs: WrappedArray[A] =>
        scala.compat.Platform.arraycopy(xs.array, 0, array, n, len)
      case _ =>
        elems.copyToArray(array, n)
    }
    size0 += len
  }
  
  /** Removes the element on a given index position. It takes time linear in
   *  the buffer size.
   *
   *  @param n  the index which refers to the first element to delete.
   *  @param count   the number of elemenets to delete
   *  @throws Predef.IndexOutOfBoundsException if <code>n</code> is out of bounds.
   */
  override 
  def remove(n: Int, count: Int) {
    require(count >= 0, "removing negative number of elements")
    if (n < 0 || n > size0 - count) throw new IndexOutOfBoundsException(n.toString)
    copy(n + count, n, size0 - (n + count))
    reduceToSize(size0 - count)
  }

  /** Removes the element on a given index position
   *  
   *  @param n  the index which refers to the element to delete.
   *  @return  The element that was formerly at position `n`
   */
  def remove(n: Int): A = {
    val result = apply(n)
    remove(n, 1)
    result
  }
  
  /** Defines the prefix of the string representation.
   */
  override 
  def stringPrefix: String = "ArrayList"

  /**
   * We need this toArray to export an array with the original type element instead of
   * scala.collection.TraversableOnce#toArray[B >: A : ClassTag]: Array[B]:
   * def toArray[B >: A : ClassTag]: Array[B] = {
   *   if (isTraversableAgain) {
   *     val result = new Array[B](size)
   *     copyToArray(result, 0)
   *     result
   *   }
   *   else toBuffer.toArray
   * }
   */
  def toArray: Array[A] = {
    val res = makeArray(length)
    scala.compat.Platform.arraycopy(array, 0, res, 0, length)
    res
  }
  
  def sliceToArray(start: Int, len: Int): Array[A] = {
    val res = makeArray(len)
    scala.compat.Platform.arraycopy(array, start, res, 0, len)
    res
  }

  // --- overrided methods for performance

  override 
  def head: A = {
    if (isEmpty) throw new NoSuchElementException
    else apply(0)
  }

  override 
  def last: A = {
    if (isEmpty) throw new NoSuchElementException
    else apply(size - 1)
  }

}

object AbstractArrayList extends SeqFactory[AbstractArrayList] {
  implicit def canBuildFrom[A]: CanBuildFrom[Coll, A, AbstractArrayList[A]] = new GenericCanBuildFrom[A]
  /**
   * we implement newBuilder for extending SeqFactory only. Since it's with no ClassTag arg,
   * we can only create a ArrayList[Any] instead of ArrayList[A], but we'll define another
   * apply method to create ArrayList[A]
   */
  def newBuilder[A]: Builder[A, AbstractArrayList[A]] = new ArrayList[Any]().asInstanceOf[ArrayList[A]]
}


