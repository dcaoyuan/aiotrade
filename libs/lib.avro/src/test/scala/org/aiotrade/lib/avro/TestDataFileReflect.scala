/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.aiotrade.lib.avro

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

import org.apache.avro.Schema
import org.apache.avro.file.DataFileReader;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.file.SeekableFileInput;
import org.junit.Assert;
import org.junit.Test;

import scala.collection.JavaConversions._

class FooRecord(private val fooCount: Int) {
  def this() = this(0)

  override def equals(that: Any): Boolean = {
    if (that.isInstanceOf[FooRecord]) {
      return this.fooCount == that.asInstanceOf[FooRecord].fooCount;
    }
    false;
  }

  override def hashCode: Int = {
    fooCount
  }

  override def toString: String = {
    classOf[FooRecord].getSimpleName() + "{count=" + fooCount + "}";
  }
}

class BarRecord(private val beerMsg: String) {

  def this() = this(null)

  override def equals(that: Any): Boolean = {
    if (that.isInstanceOf[BarRecord]) {
      if (this.beerMsg == null) {
        return that.asInstanceOf[BarRecord].beerMsg == null
      } else {
        return this.beerMsg.equals(that.asInstanceOf[BarRecord].beerMsg)
      }
    }
    false
  }

  override def hashCode: Int = {
    beerMsg.hashCode
  }

  override def toString: String = {
    classOf[BarRecord].getSimpleName() + "{msg=" + beerMsg + "}";
  }
}

object TestDataFileReflect {

  private val USER_HOME = System.getProperty("user.home")
  private val DIR = new File(System.getProperty("test.dir", USER_HOME + "/tmp"))
  private val FILE = new File(DIR, "test.avro")

  /*
   * Test that using multiple schemas in a file works doing a union before
   * writing any records.
   */
  @throws(classOf[IOException])
  def testMultiReflectWithUnionBeforeWriting() {
    val fos = new FileOutputStream(FILE);

    val reflectData = ReflectData.get();
    val schemas = Arrays.asList(reflectData.getSchema(classOf[FooRecord]),
                                reflectData.getSchema(classOf[BarRecord]))
    val union = Schema.createUnion(schemas);
    val writer = new DataFileWriter[Object](ReflectDatumWriter[Object](union)).create(union, fos);

    // test writing to a file
    val check = new CheckList[Object]();
    write(writer, new BarRecord("One beer please"), check);
    write(writer, new FooRecord(10), check);
    write(writer, new BarRecord("Two beers please"), check);
    write(writer, new FooRecord(20), check);
    writer.close();

    val din = ReflectDatumReader[Object]();
    val sin = new SeekableFileInput(FILE);
    val reader = new DataFileReader[Object](sin, din)
    val readerItr = reader.iterator
    var count = 0
    for (datum <- reader.iterator) {
      check.assertEquals(datum, count)
      count += 1
    }
    Assert.assertEquals(count, check.size())
    reader.close();
  }

  /*
   * Test that writing a record with a field that is null.
   */
  @throws(classOf[IOException])
  def testNull() {
    val fos = new FileOutputStream(FILE)

    val reflectData = ReflectData.AllowNull
    val schema = reflectData.getSchema(classOf[BarRecord])
    val writer = new DataFileWriter[BarRecord](ReflectDatumWriter[BarRecord](classOf[BarRecord], reflectData)).create(schema, fos);

    // test writing to a file
    val check = new CheckList[BarRecord]();
    write(writer, new BarRecord("One beer please"), check);
    // null record here, fails when using the default reflectData instance
    write(writer, new BarRecord(), check);
    write(writer, new BarRecord("Two beers please"), check);
    writer.close();

    val din = ReflectDatumReader[BarRecord]();
    val sin = new SeekableFileInput(FILE);
    val reader = new DataFileReader[BarRecord](sin, din);
    var count = 0
    for (datum <- reader.iterator) {
      check.assertEquals(datum, count)
      count += 1
    }
    Assert.assertEquals(count, check.size());
    reader.close();
  }

  /*
   * Test that writing out and reading in a nested class works
   */
  @throws(classOf[IOException])
  def testNestedClass() {
    val fos = new FileOutputStream(FILE);

    val schema = ReflectData.get().getSchema(classOf[BazRecord]);
    val writer = new DataFileWriter[BazRecord](ReflectDatumWriter[BazRecord](schema)).create(schema, fos);

    // test writing to a file
    val check = new CheckList[BazRecord]()
    write(writer, new BazRecord(10), check)
    write(writer, new BazRecord(20), check)
    writer.close();

    val din = ReflectDatumReader[BazRecord]()
    val sin = new SeekableFileInput(FILE)
    val reader = new DataFileReader[BazRecord](sin, din);
    var count = 0
    for (datum <- reader.iterator) {
      check.assertEquals(datum, count)
      count += 1
    }
    Assert.assertEquals(count, check.size())
    reader.close
  }

  @throws(classOf[IOException])
  private def write[T](writer: DataFileWriter[T], o: T, l: CheckList[T]) {
    writer.append(l.addAndReturn(o));
  }

  private class CheckList[T] extends java.util.ArrayList[T] {
    def addAndReturn(check: T): T = {
      add(check);
      return check;
    }

    def assertEquals(toCheck: Object, i: Int) {
      Assert.assertNotNull(toCheck);
      val o = get(i);
      Assert.assertNotNull(o);
      Assert.assertEquals(toCheck, o);
    }
  }

  private class BazRecord(private val nbr: Int) {

    def this() = this(0)

    override def equals(that: Any): Boolean = {
      if (that.isInstanceOf[BazRecord]) {
        return this.nbr == that.asInstanceOf[BazRecord].nbr;
      }
      return false;
    }

    override def hashCode: Int = {
      return nbr;
    }

    override def toString: String = {
      return classOf[BazRecord].getSimpleName() + "{cnt=" + nbr + "}";
    }
  }
}

