package com.needstyping

import akka.actor.ActorRef
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.needstyping.WorkflowActor.CreateWorkflow
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpec}

class WorkflowRoutesSpec extends WordSpec with Matchers with ScalaFutures with ScalatestRouteTest with WorkflowRoutes {

  override val workflowActor: ActorRef = system.actorOf(WorkflowActor.props, "workflowActorProps")

  lazy val routes = workflowRoutes

  "WorkflowRoutes" should {

    "create a new workflow for a given number of steps" in {
      val entity = Marshal(CreateWorkflow(5)).to[MessageEntity].futureValue
      val request = Post("/workflows").withEntity(entity)

      request ~> routes ~> check {
        status shouldEqual StatusCodes.Created
        contentType shouldEqual ContentTypes.`application/json`
        val wf = entityAs[Workflow]
        wf.id should startWith ("WF")
        wf.numberOfSteps shouldEqual 5
      }
    }
  }
}