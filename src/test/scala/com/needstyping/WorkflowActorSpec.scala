package com.needstyping

import java.time.LocalDateTime

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.needstyping.WorkflowActor._
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class WorkflowActorSpec extends TestKit(ActorSystem("WorkflowActorSpec")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  val probe = TestProbe()

  val actor = system.actorOf(Props(new WorkflowActor() {
    override def triggerScheduling =
      probe.ref ! RemoveFinishedExecutions
  }))

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "A Workflow actor" should {

    "schedule the RemoveFinishedExecutions msg" in {
      actor ! TriggerScheduling
      probe.expectMsg(RemoveFinishedExecutions)
    }

    "remove finished executions older than a minute" in {
      workflows = Map("WF1" -> Workflow("WF1", 5))
      val ex1 = "EX1" -> Execution("EX1", Workflow("WF1", 5), 2)
      val ex2 = "EX2" -> Execution("EX2", Workflow("WF1", 5), 4, LocalDateTime.now.minusMinutes(2).toString)
      val ex3 = "EX3" -> Execution("EX3", Workflow("WF1", 5), 4)
      executions = Map(ex1, ex2, ex3)

      actor ! RemoveFinishedExecutions

      awaitCond(executions == Map(ex1, ex3))
    }
  }
}

