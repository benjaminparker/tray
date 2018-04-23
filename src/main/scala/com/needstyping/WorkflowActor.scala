package com.needstyping

import akka.actor.{Actor, Props}

case class Workflow(id: String, numberOfSteps: Int)

object WorkflowActor {

  final case class CreateWorkflow(numberOfSteps: Int)

  def props: Props = Props[WorkflowActor]
}

class WorkflowActor extends Actor {

  import WorkflowActor._

  def receive: Receive = {
    case CreateWorkflow(numberOfSteps) =>
      //      create a workflow and store somehow in memory
      val newWorkflow = Workflow("some id", numberOfSteps)
      sender() ! newWorkflow
  }
}