package io.github.shopee.idata.saio

import scala.collection.mutable.ListBuffer
import org.scalatest.exceptions.TestFailedException
import scala.concurrent.{ Await, Future, Promise, duration }
import java.net.InetSocketAddress
import duration._
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import scala.concurrent.ExecutionContext.Implicits.global

class AIOTest extends org.scalatest.FunSuite {
  test("tcp server: create") {
    val server = AIO.getTcpServer()
    server.close()
  }

  test("tcp: simple message from client") {
    var p = Promise[Any]()
    var f = p.future

    val server = AIO.getTcpServer(
      onConnection = (connection: AIOConnection.Connection) => {
        ConnectionHandler(
          connection,
          onData = (data: Array[Byte]) => {
            val text = new String(data, "UTF-8")
            if (text === "hello world!") {
              p success data
            } else {
              p failure (new Exception(s"data is not correct, data is ${text}"))
            }
          }
        )
      }
    )

    AIO.getTcpClient(port = server.getPort()) map { conn =>
      val connHandler = ConnectionHandler(conn)
      connHandler.sendMessage("hello world!")
    }

    Await.result(f, Duration.Inf)
    server.close()
  }

  test("tcp: simple message from server") {
    var p = Promise[Any]()
    var f = p.future

    val server = AIO.getTcpServer(
      port = 7653,
      onConnection = (conn: AIOConnection.Connection) => {
        val connHandler = ConnectionHandler(conn)
        connHandler.sendMessage("hello world from server")
      }
    )

    AIO.getTcpClient(port = server.getPort()) map { conn =>
      ConnectionHandler(
        conn,
        onData = (data: Array[Byte]) => {
          val text = new String(data, "UTF-8")

          if (text === "hello world from server") {
            p success data
          } else {
            p failure (new Exception("data is not correct"))
          }
        }
      )
    }

    Await.result(f, Duration.Inf)
    server.close()
  }

  private def sendMessagesInOrder(connHandler: ConnectionHandler, messages: List[String]) =
    messages.foldLeft(Future { 1 })((prev, message) => {
      prev flatMap { _ =>
        connHandler.sendMessage(message) recover {
          case e: Exception => {
            println(e)
            throw e
          }
        }
      }
    })

  private def serverSendMessages(messages: List[String], clientNum: Int = 8) = {
    val server = AIO.getTcpServer(
      onConnection = (conn: AIOConnection.Connection) => {
        val connHandler = ConnectionHandler(conn)
        sendMessagesInOrder(connHandler, messages)
      }
    )

    val messageText = messages.mkString("")

    def clientGet() = {
      var p = Promise[Any]()
      var f = p.future

      val textBuilder = new StringBuilder()

      AIO.getTcpClient(port = server.getPort()) map { conn =>
        ConnectionHandler(
          conn,
          onData = (data: Array[Byte]) => {
            val text = new String(data, "UTF-8")
            textBuilder.append(text)

            if (textBuilder.length == messageText.length) {
              p success textBuilder
            }
          }
        )
      }

      f map { _ =>
        assert(textBuilder.toString() == messageText)
      }
    }

    Await.result(Future.sequence(1 to clientNum map { _ =>
      clientGet()
    }), 15.seconds)

    server.close()
  }

  private def serverGetMessages(message: String, clientNum: Int = 8) = {
    val p = Promise[Any]()
    val messageText = (1 to clientNum map { _ =>
      message
    }).mkString("")
    val builders = ListBuffer[StringBuilder]()

    val server = AIO.getTcpServer(
      onConnection = (conn: AIOConnection.Connection) => {
        val textBuilder = new StringBuilder()
        builders.append(textBuilder)
        val connHandler = ConnectionHandler(
          conn,
          onData = (data: Array[Byte]) => {
            val text = new String(data, "UTF-8")
            if (text.length > 0) {
              textBuilder.append(text)

              val curLen = builders.foldLeft(0)((prev, builder) => prev + builder.length)

              if (curLen == messageText.length) {
                p success builders
              }
            }
          }
        )
      }
    )

    def clientSend() =
      AIO.getTcpClient(port = server.getPort()) map { conn =>
        {
          val connHandler = ConnectionHandler(conn)
          connHandler.sendMessage(message)
        }
      }

    1 to clientNum foreach { _ =>
      clientSend()
    }

    Await.result(p.future, 15.seconds)
    assert(builders.map((builder) => builder.toString()).mkString("") == messageText)

    server.close()
  }

  private def clientSendMessages(messages: List[String]) = {
    var p = Promise[Any]()
    var f = p.future

    val messageText = messages.mkString("")
    val textBuilder = new StringBuilder()

    val server = AIO.getTcpServer(
      onConnection = (conn: AIOConnection.Connection) => {
        val connHandler = ConnectionHandler(
          conn,
          onData = (data: Array[Byte]) =>
            this.synchronized {
              val text = new String(data, "UTF-8")
              textBuilder.append(text)
              if (textBuilder.length == messageText.length) {
                p success textBuilder
              }
          }
        )
      }
    )

    AIO.getTcpClient(port = server.getPort()) map { conn =>
      {
        val connHandler = ConnectionHandler(conn)
        sendMessagesInOrder(connHandler, messages)
      }
    }

    Await.result(f, 15.seconds)
    server.close()

    assert(textBuilder.toString() == messageText)
  }

  test("tcp: multiple messages from server in order") {
    val messages = List("hello world from server",
                        "hello world from server again",
                        "hello world from server the third time") ++
    (1 to 1000 map { index =>
      s"hello world from server the ${index} time"
    } toList)

    serverSendMessages(messages)
  }

  test("tcp: sever get multiple messages") {
    serverGetMessages("hello world from client", 200)
  }

  test("tcp: sever get multiple big messages") {
    1 to 10 foreach { _ =>
      serverGetMessages((1 to 100 map { index =>
        s"hello world from server the ${index} time"
      }).mkString(","), 20)
    }
  }

  test("tcp: big messages from server") {
    val messages = 1 to 10 map { index =>
      (1 to 1000 map { index2 =>
        s"[${index}]hello world from server the ${index2} time"
      }).mkString(",")
    } toList

    serverSendMessages(messages)
  }

  test("tcp: multiple messages from client in order") {
    val messages = List("hello world from server",
                        "hello world from server again",
                        "hello world from server the third time") ++
    (1 to 1000 map { index =>
      s"hello world from server the ${index} time"
    } toList)

    clientSendMessages(messages)
  }

  test("tcp: big messages from client") {
    val messages = 1 to 1000 map { index =>
      (1 to 1000 map { index2 =>
        s"[${index}]hello world from server the ${index2} time"
      }).mkString(",")
    } toList

    clientSendMessages(messages)
  }

  test("tcp: on client close") {
    var p = Promise[Any]()
    var f = p.future

    val server = AIO.getTcpServer(
      onConnection = (conn: AIOConnection.Connection) => {
        ConnectionHandler(conn, onClose = (e: Exception) => {
          p trySuccess 10
        })
      }
    )

    AIO.getTcpClient(port = server.getPort()) map { conn =>
      {
        val connHandler = ConnectionHandler(conn)
        connHandler.close()
      }
    }

    Await.result(f map { data =>
      assert(data == 10)
    }, 15.seconds)
    server.close()
  }

  test("tcp: on server close") {
    var p = Promise[Any]()
    var f = p.future

    val server = AIO.getTcpServer(
      onConnection = (conn: AIOConnection.Connection) => {
        val handler = ConnectionHandler(conn)
        handler.close()
      }
    )

    AIO.getTcpClient(port = server.getPort()) map { conn =>
      {
        ConnectionHandler(conn, onClose = (e: Exception) => {
          p trySuccess 10
        })
      }
    }

    Await.result(f map { data =>
      assert(data == 10)
    }, 15.seconds)

    server.close()
  }

  test("tcp: connet fail") {
    assertThrows[java.net.ConnectException] {
      Await.result(AIO.getTcpClient(port = 12667), 15.seconds)
    }
  }
}
