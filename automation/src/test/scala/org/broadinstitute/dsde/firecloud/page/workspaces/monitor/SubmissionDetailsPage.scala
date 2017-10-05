package org.broadinstitute.dsde.firecloud.page.workspaces.monitor

import org.broadinstitute.dsde.firecloud.component.{Button, Label}
import org.broadinstitute.dsde.firecloud.config.Config
import org.broadinstitute.dsde.firecloud.page.PageUtil
import org.broadinstitute.dsde.firecloud.page.workspaces.WorkspacePage
import org.openqa.selenium.WebDriver
import org.scalatest.selenium.Page

class SubmissionDetailsPage(namespace: String, name: String)(implicit webDriver: WebDriver)
  extends WorkspacePage(namespace, name) with Page with PageUtil[SubmissionDetailsPage] {

  private val submissionId = getSubmissionId
  override val url: String = s"${Config.FireCloud.baseUrl}#workspaces/$namespace/$name/monitor/$submissionId"

  override def awaitReady(): Unit = {
    // TODO: wait on the table, once we're testing that
    submissionStatusLabel.awaitVisible()
  }

  private val submissionStatusLabel = Label("submission-status")
  private val workflowStatusLabel = Label("workflow-status")
  private val submissionIdLabel = Label("submission-id")
  private val submissionAbortButton = Button("submission-abort-button")
  private val submissionAbortModalConfirmButton = Button("submission-abort-modal-confirm-button")

  private val WAITING_STATS = Array("Queued","Launching")
  private val WORKING_STATS = Array("Submitted", "Running", "Aborting")
  private val SUCCESS_STATS = Array("Succeeded")
  private val FAILED_STATS  = Array("Failed")
  private val ABORTED_STATS  = Array("Aborted")

  private val SUBMISSION_COMPLETE_STATS = Array("Done") ++ SUCCESS_STATS ++ FAILED_STATS ++ ABORTED_STATS

  def isSubmissionDone: Boolean = {
    val status = submissionStatusLabel.getText
    SUBMISSION_COMPLETE_STATS.contains(status)
  }

  def getSubmissionId: String = {
    submissionIdLabel.getText
  }

  def verifyWorkflowSucceeded(): Boolean = {
    SUCCESS_STATS.contains(workflowStatusLabel.getText)
  }

  def verifyWorkflowFailed(): Boolean = {
    FAILED_STATS.contains(workflowStatusLabel.getText)
  }

  def verifyWorkflowAborted(): Boolean = {
    ABORTED_STATS.contains(workflowStatusLabel.getText)
  }

  def waitUntilSubmissionCompletes(): Unit = {
    while (!isSubmissionDone) {
      open
    }
  }

  def abortSubmission(): Unit = {
    submissionAbortButton.doClick()
    submissionAbortModalConfirmButton.doClick()
  }
}
