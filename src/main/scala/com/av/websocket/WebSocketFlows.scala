package com.av.websocket

import akka.http.scaladsl.model.ws.{ Message, TextMessage, BinaryMessage }
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._

trait WebSocketFlowsT {
  implicit val materializer: ActorMaterializer

  def echo: Flow[Message, Message, Any] = Flow[Message].mapConcat{
    case tm: TextMessage.Streamed => TextMessage(Source.single("You streamed: ") ++ tm.textStream) :: Nil
    case tm: TextMessage.Strict   => TextMessage(Source.single(s"You said: ${tm.text}")) :: Nil
    case bm: BinaryMessage        =>
      bm.dataStream.runWith(Sink.ignore)
      Nil
  }
}
