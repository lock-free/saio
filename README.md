# saio

AIO library for scala

## Quick example

```scala
import io.github.shopee.idata.saio.{ AIO, ConnectionHandler, AIOConnection }
import java.net.InetSocketAddress
import scala.concurrent.ExecutionContext.Implicits.global

val server = AIO.getTcpServer(
  onConnection = (connection: AIOConnection.Connection) => {
    ConnectionHandler(
      connection,
      onData = (data: Array[Byte]) => {
        val text = new String(data, "UTF-8")
        println(s"recieved data: ${text}") 
      }
    )
  }
)

val port = server.getLocalAddress().asInstanceOf[InetSocketAddress].getPort()

AIO.getTcpClient(port = port) map {
  conn =>
    val connHandler = ConnectionHandler(conn)
    connHandler.sendMessage("hello world!")
}
```
