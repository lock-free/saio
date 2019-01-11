package io.github.shopee.idata.saio

import scala.concurrent.{ ExecutionContext, Future, Promise }
import java.nio.channels.AsynchronousServerSocketChannel

object AIO {
  def getTcpClient(hostname: String = "localhost", port: Int = 8000)(
      implicit ec: ExecutionContext
  ): Future[AIOConnection.Connection] = {
    val p = Promise[AIOConnection.Connection]

    AIOClient.createConnection(hostname, port, (conn) => {
      p trySuccess conn
    }, (err) => {
      p tryFailure err
    })

    p.future
  }

  def getTcpServer(
      hostname: String = "0.0.0.0",
      port: Int = 0,
      onConnection: AIOServer.OnConnection = (conn: AIOConnection.Connection) => {}
  ): AsynchronousServerSocketChannel = AIOServer.startServer(hostname, port, onConnection)
}
