package com.needstyping

import java.time.LocalDateTime

case class Execution(id: String, workflow: Workflow, currentStep: Int = 0, creationDate: String = LocalDateTime.now.toString)

