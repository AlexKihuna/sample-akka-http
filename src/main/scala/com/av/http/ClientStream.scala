package com.av.http

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{ Success, Failure}
import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.FormData
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._

import scala.concurrent.Future

object ClientStream extends App {
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  def numberSource(): Source[Int, NotUsed] = Source.repeat(5)
  val pool = Http().cachedHostConnectionPool[Int](host="localhost", port=8080)

  def streamNumbers(num: Int): Future[(HttpRequest, Int)] = {
    Future {
      HttpRequest(
        method = HttpMethods.POST,
        uri = "/post",
        entity=FormData(Map("number"-> num.toString)).toEntity
      ) -> num
    }
  }

  numberSource()
    .mapAsync(4)(streamNumbers)
    .via(pool)
    .runForeach {
      case (Success(response), num) =>
        // TODO: also check for response status code
        println(s"Result for posting: $num was successful: $response")
        response.discardEntityBytes() // don't forget this
      case (Failure(ex), num) =>
        println(s"Posting number $num failed with $ex")
    }
}
