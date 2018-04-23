package com.needstyping

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object QuickstartServer extends App with WorkflowRoutes {

  implicit val system: ActorSystem = ActorSystem("workflowServer")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val workflowActor: ActorRef = system.actorOf(WorkflowActor.props, "workflowActor")

  lazy val routes = workflowRoutes

  Http().bindAndHandle(routes, "localhost", 9000)

  println(s"Workflow server started on http://localhost:9000/")

  Await.result(system.whenTerminated, Duration.Inf)
}
