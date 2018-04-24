package com.needstyping

import java.time.LocalDate

import akka.actor.{Actor, Props}

import scala.collection.immutable.HashMap

case class Workflow(id: String, numberOfSteps: Int)

case class Execution(id: String, workflowId: String, currentStep: Int = 0, creationDate: String = LocalDate.now.toString)

object WorkflowActor {

  case class CreateWorkflow(numberOfSteps: Int)
  case class CreateExecution(workflowId: String)
  case class IncrementStep(workflowId: String, executionId: String)
  case class WorkflowExecutionState(workflowId: String, executionId: String)
  case object StepIncremented
  case object StepNotIncremented
  case object NotFound
  case object ExecutionFinished
  case object ExecutionNotFinished

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

    case IncrementStep(wfId, exId) =>
      val workflow = workflows.get(wfId)
      val execution = executions.get(exId)
      val result = (workflow, execution) match {
        case (Some(wf), Some(ex))  =>
          if (ex.currentStep < wf.numberOfSteps - 1) {
            val updatedExecution = ex.id -> Execution(ex.id, ex.workflowId, ex.currentStep + 1, ex.creationDate)
            executions = executions - ex.id + updatedExecution
            StepIncremented
          } else
            StepNotIncremented
        case _ =>
          NotFound
      }
      sender() ! result

    case WorkflowExecutionState(wfId, exId) =>
      val workflow = workflows.get(wfId)
      val execution = executions.get(exId)
      val result = (workflow, execution) match {
        case (Some(wf), Some(ex))  =>
          if (ex.currentStep >= wf.numberOfSteps - 1) {
            ExecutionFinished
          } else
            ExecutionNotFinished
        case _ =>
          NotFound
      }
      sender() ! result
  }
}