package com.av.http

import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.StdIn
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer


object Server extends App {
  implicit val system = ActorSystem("http-system")
  implicit val materializer = ActorMaterializer()

  val route =
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

  val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)
  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine() // let it run until user presses return
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ => system.terminate()) // and shutdown when done
}
