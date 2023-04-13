package org.aiotrade.lib.io

import java.io.File

sealed abstract class FileEvent(file: File, lastModified: Long)
final case class FileAdded(file: File, lastModified: Long) extends FileEvent(file, lastModified)
final case class FileDeleted(file: File, lastModified: Long) extends FileEvent(file, lastModified)
final case class FileModified(file: File, lastModified: Long) extends FileEvent(file, lastModified)