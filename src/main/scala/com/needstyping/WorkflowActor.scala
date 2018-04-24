package com.needstyping

import java.time.LocalDateTime

import akka.actor.{Actor, ActorLogging, Props, Timers}

import scala.collection.immutable.HashMap
import scala.concurrent.duration._

case class Workflow(id: String, numberOfSteps: Int)

case class Execution(id: String, workflow: Workflow, currentStep: Int = 0, creationDate: String = LocalDateTime.now.toString)

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

  case object ScheduleKey

  case object RemoveFinishedExecutions

  case object TriggerScheduling

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
    Execution("EX" + executionId, wf)
  }

  def isFinished(wf: Workflow, ex: Execution): Boolean = ex.currentStep >= wf.numberOfSteps - 1

  def isOlderThanOneMinute(ex: Execution): Boolean = LocalDateTime.parse(ex.creationDate).isBefore(LocalDateTime.now.minusMinutes(1))
}

class WorkflowActor extends Actor with Timers with ActorLogging {

  import WorkflowActor._

  def triggerScheduling = timers.startPeriodicTimer(ScheduleKey, RemoveFinishedExecutions, 1 minute)

  def receive: Receive = {

    case TriggerScheduling => triggerScheduling

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
        case (Some(wf), Some(ex)) =>
          if (!isFinished(wf, ex)) {
            val updatedExecution = ex.id -> Execution(ex.id, ex.workflow, ex.currentStep + 1, ex.creationDate)
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
        case (Some(wf), Some(ex)) =>
          if (isFinished(wf, ex))
            ExecutionFinished
          else
            ExecutionNotFinished
        case _ =>
          NotFound
      }
      sender() ! result

    case RemoveFinishedExecutions =>
      log.info("Looking for finished executions older than 1 minute")

      val finishedIds = executions.values.filter(e => isFinished(e.workflow, e) && isOlderThanOneMinute(e)).map(_.id)
      if (finishedIds.nonEmpty) {
        log.info("Removing executions with IDs: " + finishedIds.mkString(","))
        executions = executions -- finishedIds
      }
  }
}