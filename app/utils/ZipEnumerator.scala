package utils

import java.io.{ByteArrayOutputStream, InputStream}
import java.util.zip.{ZipEntry, ZipOutputStream}

import play.api.libs.iteratee._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Play iteratee-based reactive zip-file generation.
  */
object ZipEnumerator {

  case class Source(filename: String, stream: () => Future[Option[InputStream]])



  /**
    * Given sources, returns an Enumerator that feeds a zip-file of the sources.
    */
  def apply(sources: Iterable[Source])(implicit ec: ExecutionContext): Enumerator[Array[Byte]] = {
    val resolveSources: Enumerator[ResolvedSource] = Enumerator.unfoldM(sources) { sources =>
      sources.headOption match {
        case None => Future(None)
        case Some(Source(filename, futureStream)) =>
          futureStream().map { (a: Option[InputStream]) =>
            a.map((stream: InputStream) => (sources.tail, ResolvedSource(filename, stream)))
          }
      }
    }

    val buffer = new ZipBuffer(1024*1024*8)
    val finalBits = Enumerator.generateM(Future {
      if (buffer.isClosed) None
      else {
        buffer.close
        Some(buffer.bytes)
      }
    })

    resolveSources &> zipeach(buffer) andThen finalBits
  }


  private def zipeach(buffer: ZipBuffer)(implicit ec: ExecutionContext): Enumeratee[ResolvedSource, Array[Byte]] = {
    Enumeratee.mapConcat[ResolvedSource] { source =>
      buffer.zip.putNextEntry(new ZipEntry(source.filename))
      var done = false

      def restOfStream: Stream[Array[Byte]] = {
        if (done) Stream.empty
        else {
          while (!done && !buffer.full) {
            val byte = source.stream.read
            if (byte == -1) {
              done = true
              buffer.zip.closeEntry
              source.stream.close
            }
            else buffer.zip.write(byte)
          }
          buffer.bytes #:: restOfStream
        }
      }

      restOfStream
    }
  }


  private case class ResolvedSource(filename: String, stream: InputStream)


  private class ZipBuffer(maxsize: Int) {
    private val buf = new ByteArrayOutputStream(maxsize)
    val zip = new ZipOutputStream(buf)
    private var _closed = false

    def close(): Unit = {
      if (!_closed) {
        _closed = true
        reset
        zip.close // writes central directory
      }
    }

    def isClosed = _closed

    def reset: Unit = buf.reset

    def full: Boolean = buf.size >= maxsize

    def bytes: Array[Byte] = {
      val result = buf.toByteArray
      reset
      result
    }
  }

}

