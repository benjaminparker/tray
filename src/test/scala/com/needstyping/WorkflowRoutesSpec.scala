package com.needstyping

import akka.actor.ActorRef
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.needstyping.WorkflowActor._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpec}

class WorkflowRoutesSpec extends WordSpec with Matchers with ScalaFutures with ScalatestRouteTest with WorkflowRoutes with BeforeAndAfterEach {
  override val workflowActor: ActorRef = system.actorOf(WorkflowActor.props, "workflowActorProps")

  lazy val routes = workflowRoutes

  override def beforeEach = {
    workflows = Map.empty
    executions = Map.empty
  }

  "Create new workflow" should {

    "create a new workflow for a given number of steps" in {
      val entity = Marshal(CreateWorkflow(5)).to[MessageEntity].futureValue
      val request = Post("/workflows").withEntity(entity)

      request ~> routes ~> check {
        status shouldEqual StatusCodes.Created
        contentType shouldEqual ContentTypes.`application/json`
        val response = entityAs[String]
        response shouldEqual """{"workflow_id":"WF1"}"""
        workflows.size shouldEqual 1
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
      //set up test data - public exposure of workflows doesn't feel nice - perhaps a dependency injection/stub model
      WorkflowActor.workflows = Map("WF76" -> Workflow("WF76", 0))

      val request = Post("/workflows/WF76/executions")

      request ~> routes ~> check {
        status shouldEqual StatusCodes.Created
        contentType shouldEqual ContentTypes.`application/json`
        val response = entityAs[String]
        response shouldEqual """{"workflow_execution_id":"EX1"}"""
        executions.size shouldEqual 1
      }
    }
  }

  "Increment current step" should {

    "return a 404 NOT FOUND for a non-existant workflow" in {
      val request = Put("/workflows/WFNotFound/executions/EX3")

      request ~> routes ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }

    "return a 404 NOT FOUND for a non-existant execution" in {

      WorkflowActor.workflows = Map("WF22" -> Workflow("WF22", 4))
      WorkflowActor.executions = Map("EX1" -> Execution("EX1", Workflow("WF22", 4)))

      val request = Put("/workflows/WF22/executions/EX2")

      request ~> routes ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }

    "increase the current step by 1 and then fail when number of steps is exceeded " in {

      WorkflowActor.workflows = Map("WF3" -> Workflow("WF3", 5))
      WorkflowActor.executions = Map("EX1" -> Execution("EX1", Workflow("WF3", 5), 3))

      Put("/workflows/WF3/executions/EX1") ~> routes ~> check {
        status shouldEqual StatusCodes.NoContent
      }

      Put("/workflows/WF3/executions/EX1") ~> routes ~> check {
        status shouldEqual StatusCodes.BadRequest
      }
    }
  }

  "Get execution status" should {

    "return a 404 NOT FOUND for a non-existant workflow" in {
      val request = Get("/workflows/WFNotFound/executions/EX3")

      request ~> routes ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }

    "return a 404 NOT FOUND for a non-existant execution" in {

      WorkflowActor.workflows = Map("WF22" -> Workflow("WF22", 4))
      WorkflowActor.executions = Map("EX1" -> Execution("EX1", Workflow("WF22", 4)))

      val request = Get("/workflows/WF22/executions/EX7")

      request ~> routes ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }

    "return finished equal true for current step >= number of steps - 1" in {

      WorkflowActor.workflows = Map("WF5" -> Workflow("WF5", 3))
      WorkflowActor.executions = Map("EX1" -> Execution("EX1", Workflow("WF5", 3), 2))

      val request = Get("/workflows/WF5/executions/EX1")

      request ~> routes ~> check {
        status shouldEqual StatusCodes.OK
        contentType shouldEqual ContentTypes.`application/json`
        val response = entityAs[String]
        response shouldEqual """{"finished":true}"""
      }
    }

    "return finished equal false for current step < number of steps - 1" in {

      WorkflowActor.workflows = Map("WF5" -> Workflow("WF5", 3))
      WorkflowActor.executions = Map("EX1" -> Execution("EX1", Workflow("WF5", 3), 1))

      val request = Get("/workflows/WF5/executions/EX1")

      request ~> routes ~> check {
        status shouldEqual StatusCodes.OK
        contentType shouldEqual ContentTypes.`application/json`
        val response = entityAs[String]
        response shouldEqual """{"finished":false}"""
      }
    }
  }
}