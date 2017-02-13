package com.av.http

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ Promise, Future }
import scala.util.{Failure, Success}
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.{ActorMaterializer, OverflowStrategy, QueueOfferResult}
import akka.stream.scaladsl._


object ClientPool extends App {
  implicit val system = ActorSystem("pool-system")
  implicit val materializer = ActorMaterializer()
  val queueSize = 10
  val pool = Http().cachedHostConnectionPool[Promise[HttpResponse]]("goodmanga.net")
  val queue = Source.queue(queueSize, OverflowStrategy.dropNew)
    .via(pool)
    .toMat(
      Sink.foreach({
        case ((Success(resp), p)) => p.success(resp)
        case ((Failure(e), p))    => p.failure(e)
      })
    )(Keep.left)
    .run()

  def queueRequest(request: HttpRequest): Future[HttpResponse] = {
    val responsePromise = Promise[HttpResponse]()
    queue.offer(request -> responsePromise).flatMap {
      case QueueOfferResult.Enqueued    => responsePromise.future
      case QueueOfferResult.Dropped     => Future.failed(new RuntimeException("Queue overflowed. Try again later."))
      case QueueOfferResult.Failure(ex) => Future.failed(ex)
      case QueueOfferResult.QueueClosed => Future.failed(new RuntimeException("Queue was closed (pool shut down) while running the request. Try again later."))
    }
  }

  val responseFuture: Future[HttpResponse] = queueRequest(HttpRequest(uri="/3/naruto"))
  responseFuture andThen {
    case Success(x) =>
      println(x)
    case Failure(x) => println(x.getMessage)
  } andThen {
    case _ => system.terminate()
  }
}
