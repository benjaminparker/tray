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
import spray.json._

import scala.concurrent.duration._
import scala.util.{Failure, Success}

trait WorkflowRoutes extends JsonSupport {

  implicit def system: ActorSystem

  implicit lazy val timeout = Timeout(5.seconds)

  def workflowActor: ActorRef

  lazy val workflowRoutes: Route =
    pathPrefix("workflows") {
      concat(
        pathEnd {
          post {
            entity(as[CreateWorkflow]) { createWorkflow =>
              val workflowCreated = (workflowActor ? createWorkflow).mapTo[Workflow]
              onSuccess(workflowCreated) { workflow =>
                val json = JsObject("workflow_id" -> JsString(workflow.id))
                complete(StatusCodes.Created, json)
              }
            }
          }
        },
        pathPrefix(Segment / "executions") { workflowId =>
          concat(
            pathEnd {
              post {
                val maybeExecution = (workflowActor ? CreateExecution(workflowId)).mapTo[Option[Execution]]
                onComplete(maybeExecution) {
                  case Success(Some(e: Execution)) =>
                    val json = JsObject("workflow_execution_id" -> JsString(e.id))
                    complete(StatusCodes.Created, json)
                  case Success(None) =>
                    complete(StatusCodes.NotFound)
                  case Failure(_) =>
                    complete(StatusCodes.InternalServerError)
                }
              }
            },
            path(Segment) { executionId =>
              put {
                val result = workflowActor ? IncrementStep(workflowId, executionId)
                onComplete(result) {
                  case Success(StepIncremented) =>
                    complete(StatusCodes.NoContent)
                  case Success(StepNotIncremented) =>
                    complete(StatusCodes.BadRequest)
                  case Success(NotFound) =>
                    complete(StatusCodes.NotFound)
                  case _ =>
                    complete(StatusCodes.InternalServerError)
                }
              }
            }
          )
        }
      )
    }
}
