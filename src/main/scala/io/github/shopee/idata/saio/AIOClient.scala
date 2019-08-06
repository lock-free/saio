package io.github.free.lock.saio

import java.nio.channels.AsynchronousSocketChannel
import java.net.InetSocketAddress
import java.nio.channels.CompletionHandler
import java.nio.ByteBuffer

object AIOClient {
  def createConnection(hostname: String = "localhost",
                       port: Int = 8000,
                       onComplete: (AIOConnection.Connection) => _,
                       onFail: (Throwable) => _) = {
    val clientChannel = AsynchronousSocketChannel.open()
    clientChannel.connect(new InetSocketAddress(hostname, port),
                          null,
                          new ConnectCompletionHandler(() => {
                            onComplete(AIOConnection.Connection(clientChannel))
                          }, onFail))
  }

  case class ConnectCompletionHandler(onComplete: () => _, onFail: (Throwable) => _)
      extends CompletionHandler[Void, Object] {
    def completed(result: Void, at: Object) {
      onComplete()
    }

    def failed(ex: Throwable, at: Object) {
      onFail(ex)
    }
  }
}
