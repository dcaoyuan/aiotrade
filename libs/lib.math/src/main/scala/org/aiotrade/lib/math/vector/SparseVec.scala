/*
 * Copyright (c) 2006-2007, AIOTrade Computing Co. and Contributors
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 *  o Redistributions of source code must retain the above copyright notice, 
 *    this list of conditions and the following disclaimer. 
 *    
 *  o Redistributions in binary form must reproduce the above copyright notice, 
 *    this list of conditions and the following disclaimer in the documentation 
 *    and/or other materials provided with the distribution. 
 *    
 *  o Neither the name of AIOTrade Computing Co. nor the names of 
 *    its contributors may be used to endorse or promote products derived 
 *    from this software without specific prior written permission. 
 *    
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.aiotrade.lib.math.vector

import java.util.Random
import java.util.StringTokenizer
import org.aiotrade.lib.math.timeseries.Null
import org.aiotrade.lib.collection.ArrayList

/**
 * Sparse implement of Vec. It do not store 0 valued elements.
 *
 * This class should keep elements index sorted.
 *
 * Create a <code>SparseVec</code> whose items are copied from
 * <code>source</code>.
 *
 * @param source   the array from which items are copied
 *
 * @author Caoyuan Deng
 */
class SparseVec(src: Array[VecItem]) extends Vec {
  import SparseVec._

  private var _items: Array[VecItem] = src
  private var _dimension: Int = _
    
  /**
   * Create a zero items <code>SparseVec</code>.
   */
  def this() = {
    this(new Array[VecItem](0))
  }
    
  /**
   * Create a <code>SparseVec</code> of the desired dimension initialized to zero.
   *
   * @param dimension   the dimension of the new <code>SparseVec</code>
   */
  def this(dimension: Int) = {
    this(new Array[VecItem](0))
    _dimension = dimension
  }
    
  /**
   * Create a <code>SparseVec</code> whose items are copied from
   * <code>src</code>.
   *
   * @param src   the <code>Vec</code> to be used as src
   */
  def this(src:Vec) {
    this(null.asInstanceOf[Array[VecItem]])
    copy(src)
  }
    
  def dimension = _dimension
  def dimension_=(dimension: Int) {
    this._dimension = dimension
  }

  def setTo(src: Array[VecItem]) {
    _items = src
  }
    
  def add(value: Double): Unit = {
    assert(false, "SparseVec do not support this method, because we should make sure the elements is index sorted")
  }
    
  def values: Array[Double] = {
    val _values = new Array[Double](dimension)
        
    /** as all values has been initialed to 0 , we only need to: */
    for (item <- _items) {
      _values(item.index) = item.value
    }
        
    _values
  }
  
  def values_=(values: Array[Double]) {
    if (dimension != values.length) {
      throw new ArrayIndexOutOfBoundsException("Doing operations with source of different sizes.");
    }
        
    val newItems = new Array[VecItem](dimension)
    for (i <- 0 until dimension) {
      val value = values(i)
      if (value != 0) {
        newItems(i) = VecItem(i, value)
      }
    }
        
    _items = newItems
  }
    
  def checkDimensionEquality(comp: Vec): Unit = {
    if (comp.dimension != this.dimension) {
      throw new ArrayIndexOutOfBoundsException("Doing operations with SparseVec instances of different sizes.");
    }
  }

  override 
  def clone: SparseVec = {
    new SparseVec(this)
  }
    
  def metric(other: Vec): Double = {
    this.minus(other).normTwo
  }
    
  def equals(another: Vec): Boolean = {
    if (dimension != another.dimension) {
      return false
    }

    another match {
      case x: SparseVec =>
        val itemsA = this._items
        val itemsB = x._items
        val lenA = itemsA.length
        val lenB = itemsB.length
        var idxA = 0
        var idxB = 0
        while (idxA < lenA && idxB < lenB) {
          val itemA = itemsA(idxA)
          val itemB = itemsB(idxB)
                
          if (itemA.index == itemB.index) {
            if (itemA.value != itemB.value) {
              return false
            }
            idxA += 1
            idxB += 1
          } else if (itemA.index > itemB.index) {
            idxB +=1
          } else {
            idxA +=1
          }
        }
            
      case _ =>
            
        for (i <- 0 until dimension) {
          if (apply(i) != another(i)) {
            return false
          }
        }
            
    }
        
    true
  }
    
  def itemOfByPosition(position: Int): VecItem = {
    _items(position)
  }
    
  def apply(dimensionIdx: Int): Double = {
    var i = 0
    while (i < _items.length) {
      if (_items(i).index == dimensionIdx) {
        return _items(i).value
      }
      i += 1
    }
      
    0.0
  }
    
  def update(dimensionIdx: Int, value: Double) {
    val itemIdx = itemIdxOf(dimensionIdx)
    if (itemIdx >= 0) {
      _items(itemIdx) = VecItem(dimension, value)
    } else {
      val newItems = new Array[VecItem](_items.length + 1)
      var added = false
      var i = 0
      while (i < newItems.length) {
        if (_items(i).index < dimensionIdx) {
          newItems(i) = _items(i)
        } else {
          if (!added) {
            newItems(i) = VecItem(dimensionIdx, value)
            added = true
          } else {
            newItems(i) = _items(i - 1)
          }
        }
        i += 1
      }
            
      _items = newItems
    }
  }
    
  private def itemIdxOf(dimensionIdx: Int): Int = {
    var i = 0
    while (i < _items.length) {
      if (_items(i).index == dimensionIdx) {
        return _items(i).index
      }
      i += 1
    }
   
    -1
  }

  def itemOf(dimensionIdx: Int): VecItem = {
    val i = itemIdxOf(dimensionIdx)
    if (i >= 0) {
      _items(i)
    } else {
      null
    }
  }
    
  def setAll(value: Double) {
    if (value == 0) {
            
      _items = null
            
    } else {
            
      _items = new Array[VecItem](dimension)
      var i = 0
      while (i < dimension) {
        _items(i) = VecItem(i, value)
        i += 1
      }
    }
  }
    
  def copy(src:Vec) {
    checkDimensionEquality(src)

    _items = src match {
      case x:SparseVec =>
        val srcItems = x._items
        val newItems = new Array[VecItem](srcItems.length)
        System.arraycopy(srcItems, 0, newItems, 0, srcItems.length)

        newItems

      case _ =>
        val itemBuf = new ArrayList[VecItem]
        var i = 0
        while (i < src.dimension) {
          val value = src(i)
          if (value != 0) {
            itemBuf += VecItem(i, value)
          }
          
          i += 1
        }

        itemBuf.toArray
    }
  }
    
  def copy(src: Vec, srcPos: Int, destPos: Int, length: Int) {
    /** todo */
    //System.arraycopy(src.toDoubleArray(), srcPos, items, destPos, length);
  }
    
    

    
  def plus(operand: Vec): Vec = {
    checkDimensionEquality(operand)
        
    val result = new SparseVec(dimension)
        
    for (i <- 0 until dimension) {
      val value = this(i) + operand(i)
      if (value != 0) {
        result(i) = value
      }
    }
        
    result
  }
    
  def minus(operand: Vec): Vec = {
    checkDimensionEquality(operand)
        
    val result = new SparseVec(dimension)
        
    for (i <- 0 until operand.dimension) {
      val value = this(i) - operand(i)
      if (value != 0) {
        result(i) = value
      }
    }
        
    result
  }
    
  def innerProduct(operand:Vec): Double = {
    checkDimensionEquality(operand)
        
    var result = 0d

    operand match {
      case x: SparseVec =>
        /** A quick algorithm in case of both are SparseVec */
        val itemsA = this._items
        val itemsB = x._items
        val lenA = itemsA.length
        val lenB = itemsB.length
        var idxA = 0
        var idxB = 0
        while (idxA < lenA && idxB < lenB) {
          val itemA = itemsA(idxA)
          val itemB = itemsB(idxB)
                
          if (itemA.index == itemB.index) {
            result += itemA.value * itemB.value
            idxA += 1
            idxB +=1
          } else if (itemA.index > itemB.index) {
            idxB += 1
          } else {
            idxA += 1
          }
        }
            
      case _ =>
            
        /** for inner product, we only need compute with those value != 0 */
        for (i <- 0 until _items.length) {
          val item = _items(i)
          result += item.value * operand(item.index)
        }
            
    }
        
    result
  }
    
  def square: Double = {
    var result = 0d
        
    var i = 0
    while (i < _items.length) {
      val value = _items(i).value
      result += value * value
      i += 1
    }
        
    result
  }
    
  def plus(operand: Double): Vec = {
    val result = new SparseVec(this)
        
    var i = 0 
    while (i < _items.length) {
      val item = _items(i)
      result._items(i) = VecItem(item.index, item.value + operand)
      i += 1
    }
        
    result
  }
    
    
  def times(operand: Double): Vec = {
    val result = new SparseVec(this)
        
    var i = 0
    while (i < _items.length) {
      val item = _items(i)
      result._items(i) = VecItem(item.index, item.value * operand)
      i += 1
    }
        
    result
  }
    
  def compactSize: Int = {
    _items.length
  }
    
  def compactData: Array[VecItem] = {
    _items
  }
    
  def normOne: Double = {
    var result = 0d
        
    /** for norm1 operation, we only need compute with those data.value != 0 */
    var i = 0
    while (i < _items.length) {
      result += math.abs(_items(i).value)
      i += 1
    }
        
    result
  }
    
  def normTwo: Double = {
    var result = 0.0
        
    /** for norm2 operation, we only need compute with those data.value != 0 */
    var i = 0
    while (i < _items.length) {
      result += math.pow(_items(i).value, 2)
      i += 1
    }
    result = math.sqrt(result)
        
    result
  }
    
  def checkValidation: Boolean = {
    var i = 0
    while (i < _items.length) {
      if (Null.is(_items(i).value)) {
        return false
      }
      i += 1
    }
        
    true
  }

  def randomize(min: Double, max: Double) {
    val source = new Random(System.currentTimeMillis + Runtime.getRuntime.freeMemory)

    var i = 0
    while (i < dimension) {
      /**
       * @NOTICE
       * source.nextDouble() returns a pseudorandom value between 0.0 and 1.0
       */
      update(i, source.nextDouble * (max - min) + min)
      i += 1
    }
  }


  override 
  def toString: String = {
    val sb = new StringBuffer()
        
    sb.append("[")
    var i = 0
    while (i < dimension) {
      sb.append(this(i)).append(ITEM_SEPARATOR)
      i += 1
    }
    sb.append("]")
        
    sb.toString
  }
    
    
}

object SparseVec {
  val ITEM_SEPARATOR = " "

  /**
   * Parses a String into a <code>DefaultVec</code>.
   * Elements are separated by <code>DefaultVec.ITEM_SEPARATOR</code>
   *
   * @param str   the String to parse
   * @return the resulting <code>DefaultVec</code>
   * @see DefaultVec#ITEM_SEPARATOR
   */
  def parseVec(str: String): Vec = {
    val st = new StringTokenizer(str, ITEM_SEPARATOR)

    val dimension = st.countTokens

    val result = new DefaultVec(dimension)

    for (i <- 0 until dimension) {
      result(i) = st.nextToken.toDouble
    }

    result
  }
}
