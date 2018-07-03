package com.github.jogoes.awstextrekognition

import java.io._
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

import scala.annotation.tailrec
import scala.util.Try

object IOHelper {

  def withCloseable[R <: Closeable, A](r: R)(f: R => A): Try[A] = {
    val a = Try(f(r))
    r.close()
    a
  }

  def withResource[R, A](r: R)(close: R => Unit)(f: R => A): Try[A] = {
    val a = Try(f(r))
    close(r)
    a
  }

  def toByteArray(is: InputStream): Array[Byte] = {
    val buffer = new Array[Byte](2048)
    val bos = new ByteArrayOutputStream()

    @tailrec
    def read(): Unit = {
      val n = is.read(buffer, 0, buffer.length)
      if (n != -1) {
        bos.write(buffer, 0, n)
        read()
      }
    }

    read()
    bos.toByteArray
  }

  def readFile(file: File, maxSize: Int): Try[Option[ByteBuffer]] = {

    def read(channel: FileChannel): Option[ByteBuffer] = {
      val fileSize = channel.size()
      if (fileSize > maxSize) None
      else {
        val buffer = ByteBuffer.allocate(fileSize.toInt)
        channel.read(buffer)
        buffer.flip()
        Some(buffer)
      }
    }

    withCloseable(new RandomAccessFile(file, "r")) {
      f => withCloseable(f.getChannel)(read)
    }.flatten
  }
}
