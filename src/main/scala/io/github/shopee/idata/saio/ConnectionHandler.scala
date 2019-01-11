package io.github.shopee.idata.saio

import java.nio.ByteBuffer
import scala.concurrent.{ ExecutionContext, Future, Promise }

case class ConnectionHandler(
    connection: AIOConnection.Connection,
    onData: (Array[Byte]) => _ = (b: Array[Byte]) => {},
    onClose: (Exception) => _ = (e: Exception) => {}
)(implicit ec: ExecutionContext) {
  private def readData(): Unit =
    connection.readChunk((bytes: Array[Byte]) => {
      if(bytes != null) {
        onData(bytes)
      }
      // keep reading data
      readData()
    }, (e: Exception) => {
      // close current connection
      close(e)
    })

  readData()

  def sendMessage(msg: String) = sendBytes(ByteBuffer.wrap(msg.getBytes()))

  def sendBytes(bytes: ByteBuffer): Future[Int] = {
    val p = Promise[Int]()
    connection.write(bytes, () => {
      p trySuccess 1
    }, (e: Exception) => {
      p tryFailure e
      // close current connection
      close(e)
    })
    p.future
  }

  def close(e: Exception = null) = {
    connection.close()
    onClose(e)
  }
}
