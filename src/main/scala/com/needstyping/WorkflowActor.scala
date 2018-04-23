package com.needstyping

import akka.actor.{Actor, Props}

import scala.collection.immutable.HashMap

case class Workflow(id: String, numberOfSteps: Int)


object WorkflowActor {
  final case class CreateWorkflow(numberOfSteps: Int)

  val workflows : Map[String, Workflow] = HashMap.empty
  var id = 0
  def props: Props = Props[WorkflowActor]

  def createWF(numberOfSteps: Int): Workflow = {
    id += 1
    Workflow("WF" + id, numberOfSteps)
  }
}

class WorkflowActor extends Actor {

  import WorkflowActor._

  def receive: Receive = {
    case CreateWorkflow(numberOfSteps) =>
      val wf = createWF(numberOfSteps)
      workflows == workflows + (wf.id -> wf)
      sender() ! wf
  }
}