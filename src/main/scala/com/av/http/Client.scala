package com.av.http

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{ Success, Failure }

object Client extends App {
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  val responseFuture: Future[HttpResponse] = Http().singleRequest(HttpRequest(uri = "http://goodmanga.net/3/naruto"))
  responseFuture andThen {
    case Success(x) =>
      println(x)
      println(x.entity)
    case Failure(x) => println(x.getMessage)
  } andThen {
    case _ => system.terminate()
  }
}
