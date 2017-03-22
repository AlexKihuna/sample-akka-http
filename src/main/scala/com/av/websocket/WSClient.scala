package com.av.websocket

import akka.{ Done, NotUsed }
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._

import scala.concurrent.Future
import scala.util.{ Failure, Success }

object WSClient extends App {
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  import system.dispatcher

  val printSink: Sink[Message, Future[Done]] =
    Sink.foreach {
      case message: TextMessage.Strict   => println(message.text)
      case message: TextMessage.Streamed => message.textStream.runFold("")(_ + _).flatMap(msg => Future.successful(msg)).map(println)
    }

  val helloSource: Source[Message, NotUsed] =
    Source.single(TextMessage("Strict hello world!"))

  val helloStreamSource: Source[Message, NotUsed] =
    Source.single(TextMessage(Source(for(i <- 1 to 100) yield i.toString)))


  val flow: Flow[Message, Message, Future[Done]] =
  Flow.fromSinkAndSourceMat(printSink, helloSource)(Keep.left)

  val (upgradeResponse, closed) =
    Http().singleWebSocketRequest(
      request = WebSocketRequest(
        "ws://localhost:9080/wsecho"
      ),
      clientFlow = flow
    )

  val connected = upgradeResponse.map {
    case v: ValidUpgrade           => v.response
    case r: InvalidUpgradeResponse => throw new RuntimeException(s"Caused by ${r.cause}. Resp: ${r.response}")
  }

  val webSocketFlow = Http().webSocketClientFlow(
    WebSocketRequest(
      uri = "ws://localhost:9080/wsechoSub",
      subprotocol = Some("support")
    )
  )
  val (flowUpgradeResponse, flowClosed) =
    helloSource
      .viaMat(webSocketFlow)(Keep.right)
      .toMat(printSink)(Keep.both)
      .run()

  val webSocketFlow2 = Http().webSocketClientFlow(
    WebSocketRequest(
      uri = "ws://localhost:9080/wsechoOptSub",
      subprotocol = Some("help")
    )
  )
  val (flowUpgradeResponse2, flowClosed2) =
    helloStreamSource
      .viaMat(webSocketFlow2)(Keep.right)
      .toMat(printSink)(Keep.both)
      .run()

  val responseFut = for {
   x <- upgradeResponse
   y <- flowUpgradeResponse
   z <- flowUpgradeResponse2
  } yield (x, y, z)

  val closing = for {
    s <- closed
    d <- flowClosed
    f <- flowClosed2
  } yield (s, d, f)

  responseFut onComplete{
    case Success(x)  =>
      x.productIterator.map {
        case v: ValidUpgrade           => v.response
        case r: InvalidUpgradeResponse => throw new RuntimeException(s"Caused by ${r.cause}. Resp: ${r.response}")
      }.foreach(println)
    case Failure(ex) =>
      println(ex.getMessage)
  }

  closing.onComplete(_ => system.terminate())
}
