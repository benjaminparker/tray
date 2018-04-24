package com.needstyping

import java.time.LocalDate

import akka.actor.{Actor, Props}

import scala.collection.immutable.HashMap

case class Workflow(id: String, numberOfSteps: Int)
case class Execution(id: String, workflowId: String, currentStep: Int = 0, creationDate: String = LocalDate.now.toString)

object WorkflowActor {

  case class CreateWorkflow(numberOfSteps: Int)
  case class CreateExecution(workflowId: String)

  //These horrible vars are needed as we are using memory as a data store - alternative is mutable map which seems worse and not thread safe
  var workflows: Map[String, Workflow] = HashMap.empty
  var executions: Map[String, Execution] = HashMap.empty
  private var workflowId = 0
  private var executionId = 0

  def props: Props = Props[WorkflowActor]

  def createWF(numberOfSteps: Int): Workflow = {
    workflowId += 1
    Workflow("WF" + workflowId, numberOfSteps)
  }

  def createExecution(wf: Workflow): Execution = {
    executionId += 1
    Execution("EX" + executionId, wf.id)
  }
}

class WorkflowActor extends Actor {

  import WorkflowActor._

  def receive: Receive = {
    case CreateWorkflow(numberOfSteps) =>
      val wf = createWF(numberOfSteps)
      workflows = workflows + (wf.id -> wf)
      sender() ! wf

    case CreateExecution(wfId) =>
      val execution = workflows.get(wfId) map createExecution
      execution foreach { e => executions == executions + (e.id -> e) }
      sender() ! execution
  }
}