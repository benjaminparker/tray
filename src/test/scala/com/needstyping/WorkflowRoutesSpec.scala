package com.needstyping

import akka.actor.ActorRef
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.needstyping.WorkflowActor._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpec}

class WorkflowRoutesSpec extends WordSpec with Matchers with ScalaFutures with ScalatestRouteTest with WorkflowRoutes {

  override val workflowActor: ActorRef = system.actorOf(WorkflowActor.props, "workflowActorProps")

  lazy val routes = workflowRoutes

  "Create new workflow" should {

    "create a new workflow for a given number of steps" in {
      val entity = Marshal(CreateWorkflow(5)).to[MessageEntity].futureValue
      val request = Post("/workflows").withEntity(entity)

      request ~> routes ~> check {
        status shouldEqual StatusCodes.Created
        contentType shouldEqual ContentTypes.`application/json`
        val response = entityAs[String]
        response shouldEqual """{"workflow_id":"WF1"}"""
      }
    }
  }

  "Create new execution" should {

    "return a 404 NOT FOUND for a non-existant workflow" in {
      val request = Post("/workflows/WFNonExistant/executions")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }

    "return a new execution for a given workflow" in {
      //set up test data - maybe do this in separate method
      WorkflowActor.workflows = Map("WF76" -> Workflow("WF76", 0))

      val request = Post("/workflows/WF76/executions")

      request ~> routes ~> check {
        status shouldEqual StatusCodes.Created
        contentType shouldEqual ContentTypes.`application/json`
        val response = entityAs[String]
        response shouldEqual """{"workflow_execution_id":"EX1"}"""
      }
    }
  }
}