package io.github.shopee.idata.saio

import java.nio.channels.AsynchronousServerSocketChannel
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import java.nio.channels.AsynchronousServerSocketChannel
import scala.collection.mutable.ListBuffer

object AIOServer {
  type OnConnection = (AIOConnection.Connection) => _

  case class AcceptHandler(onComplete: (AsynchronousSocketChannel, Object) => _,
                           onFail: (Throwable, Object) => _)
      extends CompletionHandler[AsynchronousSocketChannel, Object] {
    def completed(ch: AsynchronousSocketChannel, att: Object) {
      onComplete(ch, att)
    }

    def failed(ex: Throwable, att: Object) {
      onFail(ex, att)
    }
  }

  case class Server(hostname: String = "0.0.0.0", port: Int, onConnection: OnConnection) {
    val addr   = new InetSocketAddress(hostname, port)
    val server = AsynchronousServerSocketChannel.open().bind(addr)

    private val channels = ListBuffer[AIOConnection.Connection]()

    private lazy val acceptHandler: AcceptHandler = AcceptHandler(
      (ch: AsynchronousSocketChannel, att: Object) => {
        try {
          val aioConn = AIOConnection.Connection(ch)
          val lives = channels.toList.filter((channel) => channel.isOpen())
          channels.clear()
          channels.appendAll(lives)

          onConnection(aioConn)
        } finally {
          server.accept("Client connection", acceptHandler)
        }
      },
      (ex: Throwable, att: Object) => {
        // TODO fail
        if (server.isOpen()) {
          server.accept("Client connection", acceptHandler)
        }
      }
    )

    server.accept("Client connection", acceptHandler)

    def close() = {
      server.close()

      // close all channels
      channels.foreach((channel) => {
        try {
          channel.close()
        } catch {
          case e: Exception => {
            println(e)
          }
        }
      })
      channels.clear()
    }

    def getPort(): Int = server.getLocalAddress().asInstanceOf[InetSocketAddress].getPort()
  }

  def startServer(hostname: String = "0.0.0.0", port: Int, onConnection: OnConnection): Server =
    Server(hostname, port, onConnection)
}
