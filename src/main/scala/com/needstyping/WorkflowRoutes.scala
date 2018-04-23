package com.needstyping

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.post
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.pattern.ask
import akka.util.Timeout
import com.needstyping.WorkflowActor._

import scala.concurrent.duration._

trait WorkflowRoutes extends JsonSupport {

  implicit def system: ActorSystem
  implicit lazy val timeout = Timeout(5.seconds)

  def workflowActor: ActorRef

  lazy val workflowRoutes: Route =
    pathPrefix("workflows") {
      pathEnd {
        concat(
          post {
            entity(as[CreateWorkflow]) { createWorkflow =>
              val workflowCreated = (workflowActor ? createWorkflow).mapTo[Workflow]
              onSuccess(workflowCreated) { workflow =>
                complete((StatusCodes.Created, workflow))
              }
            }
          }
        )
      }
    }
}
