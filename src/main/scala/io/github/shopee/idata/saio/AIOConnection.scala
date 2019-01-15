package io.github.shopee.idata.saio

import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import java.nio.ByteBuffer

object AIOConnection {
  type FailHandler     = (Throwable, Any) => _
  type WriteCompletion = (Integer, ByteBuffer) => _
  type ReadCompletion  = (Integer, AsynchronousSocketChannel) => _

  case class WriteHandler(buffer: ByteBuffer, onComplete: WriteCompletion, onFail: FailHandler)
      extends CompletionHandler[Integer, ByteBuffer] {
    def completed(status: Integer, ch: ByteBuffer): Unit =
      onComplete(status, ch)
    def failed(ex: Throwable, ch: ByteBuffer) {
      onFail(ex, ch)
    }
  }

  case class ReadHandler(buffer: ByteBuffer, onComplete: ReadCompletion, onFail: FailHandler)
      extends CompletionHandler[Integer, AsynchronousSocketChannel] {
    def completed(status: Integer, ch: AsynchronousSocketChannel) {
      onComplete(status, ch)
    }
    def failed(ex: Throwable, ch: AsynchronousSocketChannel) {
      onFail(ex, ch)
    }
  }

  case class Connection(ch: AsynchronousSocketChannel) {
    val readBuf = ByteBuffer.allocate(1024)

    def readChunk(onData: (Array[Byte]) => _, onFail: (Exception) => _) =
      ch.read(
        readBuf,
        ch,
        ReadHandler(
          readBuf,
          (status: Integer, ch: AsynchronousSocketChannel) => {
            // success
            if (status < 0) { // connection closed
              onFail(new Exception("connection already closed"))
            } else if (status == 0) { // no data
              onData(null)
            } else {
              readBuf.flip() // read data
              val bytes = new Array[Byte](readBuf.remaining())
              readBuf.get(bytes)
              onData(bytes)
              readBuf.clear()
            }
          },
          (ex: Throwable, ch: Any) => {
            onFail(new Exception(ex))
            readBuf.clear()
          }
        )
      )

    // write data to channel
    def write(buffer: ByteBuffer, onComplete: () => _, onFail: (Exception) => _): Unit =
      ch.write(
        buffer,
        buffer,
        WriteHandler(
          buffer,
          (status: Integer, buffer: ByteBuffer) => {
            if (status < 0) {
              onFail(new Exception("connection already closed"))
            } else {
              if (buffer.hasRemaining()) {
                write(buffer, onComplete, onFail)
              } else {
                onComplete()
                buffer.clear()
              }
            }
          },
          (ex: Throwable, ch: Any) => {
            onFail(new Exception(ex))
            buffer.clear()
          }
        )
      )

    def close() = ch.close()

    def isOpen() = ch.isOpen()
  }
}
