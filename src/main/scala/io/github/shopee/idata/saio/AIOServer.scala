package io.github.shopee.idata.saio

import java.nio.channels.AsynchronousServerSocketChannel
import java.net.InetSocketAddress
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import java.nio.channels.AsynchronousServerSocketChannel

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

  def startServer(hostname: String = "0.0.0.0",
                  port: Int,
                  onConnection: OnConnection): AsynchronousServerSocketChannel = {
    val addr   = new InetSocketAddress(hostname, port)
    val server = AsynchronousServerSocketChannel.open().bind(addr)

    lazy val acceptHandler: AcceptHandler = AcceptHandler(
      (ch: AsynchronousSocketChannel, att: Object) => {
        try {
          onConnection(AIOConnection.Connection(ch))
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

    server
  }
}
