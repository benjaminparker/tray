package com.needstyping

import java.time.LocalDate

import akka.actor.{Actor, Props}

import scala.collection.immutable.HashMap

final case class Workflow(workflow_id: String, number_of_steps: Int)

final case class Execution(execution_id: String, workflow_id: String, current_step: Int = 0, creation_date: String = LocalDate.now.toString)


object WorkflowActor {

  final case class CreateWorkflow(number_of_steps: Int)

  final case class CreateExecution(workflow_id: String)

  //These horrible vars are needed as we are using memory as a data store - alternative is mutable map which seems worse and not thread safe
  var workflows: Map[String, Workflow] = HashMap.empty
  var executions: Map[String, Execution] = HashMap.empty
  var workflowId = 0
  var executionId = 0

  def props: Props = Props[WorkflowActor]

  def createWF(numberOfSteps: Int): Workflow = {
    workflowId += 1
    Workflow("WF" + workflowId, numberOfSteps)
  }

  def createExecution(wf: Workflow): Execution = {
    executionId += 1
    Execution("EX" + executionId, wf.workflow_id)
  }
}

class WorkflowActor extends Actor {

  import WorkflowActor._

  def receive: Receive = {
    case CreateWorkflow(numberOfSteps) =>
      val wf = createWF(numberOfSteps)
      workflows = workflows + (wf.workflow_id -> wf)
      sender() ! wf

    case CreateExecution(workflowId) =>
      val execution = workflows.get(workflowId) map createExecution
      execution foreach { e => executions == executions + (e.execution_id -> e) }
      sender() ! execution
  }
}