package com.needstyping

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.needstyping.WorkflowActor.CreateWorkflow
import spray.json.DefaultJsonProtocol

trait JsonSupport extends SprayJsonSupport {
  import DefaultJsonProtocol._

  implicit val workflowJsonFormat = jsonFormat2(Workflow)
  implicit val executionJsonFormat = jsonFormat4(Execution)
  implicit val createWorkflowJsonFormat = jsonFormat1(CreateWorkflow)
}
