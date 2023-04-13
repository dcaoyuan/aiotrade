/*                     __                                               *\
 **     ________ ___   / /  ___     Scala API                            **
 **    / __/ __// _ | / /  / _ |    (c) 2003-2009, LAMP/EPFL             **
 **  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
 ** /____/\___/_/ |_/____/_/ | |                                         **
 **                          |/                                          **
 \*                                                                      */

// $Id: ResizableArray.scala 19219 2009-10-22 09:43:14Z moors $


package org.aiotrade.lib.collection

import scala.collection.generic._
import scala.collection.mutable.Builder
import scala.collection.mutable.IndexedSeq
import scala.collection.mutable.IndexedSeqOptimized
import scala.reflect.ClassTag

/** This class is used internally to implement data structures that
 *  are based on resizable arrays.
 *
 *  @tparam A    type of the elements contained in this resizeable array.
 *  
 *  @author  Matthias Zenger, Burak Emir
 *  @author Martin Odersky
 *  @author Caoyuan Deng
 *  @version 2.8
 *  @since   1
 */
trait ResizableArray[A] extends IndexedSeq[A]
                           with GenericTraversableTemplate[A, ResizableArray]
                           with IndexedSeqOptimized[A, ResizableArray[A]] {

  /** trait can not have type parameters like ResizableArray[A : ClassTag], so we have define a implicit val here */
  protected implicit val m: ClassTag[A]
  
  protected val elementClass: Option[Class[A]]
  
  override 
  def companion: GenericCompanion[ResizableArray] = ResizableArray

  protected def initialSize: Int = 16
  protected[collection] var array: Array[A] = makeArray(initialSize)

  /**
   * Under case of calling new ArrayList[T] where T is not specified explicitly 
   * during compile time, ie. the T is got during runtime, 'm' may be "_ <: Any",
   * or, AnyRef. In this case, we should pass in an elementClass and create array via
   *   java.lang.reflect.Array.newInstance(x, size) 
   */
  protected def makeArray(size: Int) = {
    elementClass match {
      case Some(x) => 
        java.lang.reflect.Array.newInstance(x, size).asInstanceOf[Array[A]]
      case None => 
        // If the A is specified under compile time, that's ok, an Array[A] will be 
        // created (if A is primitive type, will also create a primitive typed array @see scala.reflect.Manifest)
        // If A is specified under runtime, will create a Array[AnyRef]
        new Array[A](size) 
    }
  }

  protected var size0: Int = 0

  //##########################################################################
  // implement/override methods of IndexedSeq[A]

  /** Returns the length of this resizable array.
   */
  def length: Int = size0

  def apply(idx: Int) = {
    if (idx >= size0) throw new IndexOutOfBoundsException(idx.toString)
    array(idx)
  }

  def update(idx: Int, elem: A) { 
    if (idx >= size0) throw new IndexOutOfBoundsException(idx.toString)
    array(idx) = elem
  }

  override 
  def foreach[U](f: A =>  U) {
    // size is cached here because profiling reports a lot of time spent calling
    // it on every iteration.  I think it's likely a profiler ghost but it doesn't
    // hurt to lift it into a local.
    var i = 0
    val top = size
    while (i < top) {
      f(array(i))
      i += 1
    }
  }

  /** Fills the given array <code>xs</code> with at most `len` elements of
   *  this traversable starting at position `start`.
   *  Copying will stop once either the end of the current traversable is reached or
   *  `len` elements have been copied or the end of the array is reached.
   *
   *  @param  xs the array to fill.
   *  @param  start starting index.
   *  @param  len number of elements to copy
   */
  override 
  def copyToArray[B >: A](xs: Array[B], start: Int, len: Int) {
    val len1 = len min (xs.length - start) min length
    scala.compat.Platform.arraycopy(array, 0, xs, start, len1)
  }

  //##########################################################################

  /** remove elements of this array at indices after <code>sz</code> 
   */
  def reduceToSize(sz: Int) {
    require(sz <= size0)
    while (size0 > sz) {
      size0 -= 1
      if (!m.runtimeClass.isPrimitive) {
        array(size0) = null.asInstanceOf[A] 
      }
    }
  }

  /** ensure that the internal array has at n cells */
  protected def ensureSize(n: Int) {
    if (n > array.length) {
      // make sure newsize is not 0 by math.max(array.length, 1)
      var newsize = math.max(array.length, 1) * 2 
      while (n > newsize)
        newsize = newsize * 2
      val newar = makeArray(newsize)
      scala.compat.Platform.arraycopy(array, 0, newar, 0, size0)
      array = newar
    }
  }

  /** Swap two elements of this array.
   */
  protected def swap(a: Int, b: Int) {
    val h = array(a)
    array(a) = array(b)
    array(b) = h
  }

  /** Move parts of the array.
   */
  protected def copy(m: Int, n: Int, len: Int) {
    scala.compat.Platform.arraycopy(array, m, array, n, len)
  }
}

object ResizableArray extends SeqFactory[ResizableArray] {
  implicit def canBuildFrom[A]: CanBuildFrom[Coll, A, ResizableArray[A]] = new GenericCanBuildFrom[A]
  def newBuilder[A]: Builder[A, ResizableArray[A]] = new ArrayList[Any].asInstanceOf[ArrayList[A]]
}
