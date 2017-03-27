package com.av.http

import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.StdIn

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer


import com.av.websocket.WebSocketFlowsT

object Server extends App with WebSocketFlowsT {
  implicit val system = ActorSystem("http-system")
  implicit val materializer = ActorMaterializer()

  val route =
    path("wsecho") {
      handleWebSocketMessages(echo)
    } ~
    path("wsechoSub"){
      handleWebSocketMessagesForProtocol(echo, "support")
    } ~
    path("wsechojanus"){
      handleWebSocketMessagesForProtocol(echo, "janus-admin-protocol")
    } ~
    path("wsechoOptSub"){
      handleWebSocketMessagesForOptionalProtocol(echo, Some("help"))
    } ~
    get {
      pathSingleSlash {
        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`,"<html><body>Hello world!</body></html>"))
      } ~
      path("ping") {
        complete("PONG!")
      } ~
      path("crash") {
        sys.error("BOOM!")
      }
    } ~
    post {
      path("post") {
        formFields('number.as[Int]){
          number => complete(s"Received $number")
        }
      }
    }

  val bindingFuture = Http().bindAndHandle(route, "localhost", 9080)
  println(s"Server online at http://localhost:9080/\nPress RETURN to stop...")
  StdIn.readLine()
  bindingFuture
    .flatMap(_.unbind())
    .onComplete(_ => system.terminate())
}
