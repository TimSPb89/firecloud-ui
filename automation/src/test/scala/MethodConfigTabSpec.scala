import java.util.UUID

import org.broadinstitute.dsde.firecloud.auth.{AuthToken, AuthTokens, Credentials}
import org.broadinstitute.dsde.firecloud.data.TestData
import org.broadinstitute.dsde.firecloud.pages.{WebBrowserSpec, WorkspaceListPage, WorkspaceMethodConfigPage, SubmissionDetailsPage}
import org.broadinstitute.dsde.firecloud.{CleanUp, Config}
import org.broadinstitute.dsde.firecloud.workspaces.WorkspaceFixtures
import org.scalatest._

class MethodConfigTabSpec extends FreeSpec with WebBrowserSpec with CleanUp with WorkspaceFixtures {

  val billingProject: String = Config.Projects.default
  val methodName: String = TestData.SimpleMethod.name + "_" + UUID.randomUUID().toString
  val methodConfigName: String = TestData.SimpleMethodConfig.name + "_" + UUID.randomUUID().toString
  val wrongRootEntityErrorText: String = "Error: Method configuration expects an entity of type sample, but you gave us an entity of type participant."
  val noExpressionErrorText: String = "Error: Method configuration expects an entity of type sample, but you gave us an entity of type sample_set."
  val missingInputsErrorText: String = "is missing definitions for these inputs:"

  implicit val authToken: AuthToken = AuthTokens.hermione
  val uiUser: Credentials = Config.Users.hermione

  "launch a simple workflow" in withWebDriver { implicit driver =>
    withWorkspace(billingProject, "TestSpec_FireCloud_launch_a_simple_workflow") { workspaceName =>
      api.importMetaData(billingProject, workspaceName, "entities", TestData.SingleParticipant.participantEntity)

      signIn(uiUser)
      val workspaceMethodConfigPage = new WorkspaceMethodConfigPage(billingProject, workspaceName).open
      val methodConfigDetailsPage = workspaceMethodConfigPage.importMethodConfigFromRepo(TestData.SimpleMethod.namespace,
        TestData.SimpleMethod.name, TestData.SimpleMethod.snapshotId, methodName, Some(TestData.SimpleMethodConfig.rootEntityType))

      methodConfigDetailsPage.editMethodConfig(inputs = Some(TestData.SimpleMethod.inputs))
      val submissionDetailsPage = methodConfigDetailsPage.launchAnalysis(TestData.SimpleMethod.rootEntityType, TestData.SingleParticipant.entityId)

      submissionDetailsPage.waitUntilSubmissionCompletes() //This feels like the wrong way to do this?
      assert(submissionDetailsPage.verifyWorkflowSucceeded())
    }
  }

  "launch modal with no default entities" in withWebDriver { implicit driver =>
    withWorkspace(billingProject, "TestSpec_FireCloud_launch_modal_no_default_entities") { workspaceName =>
      api.importMetaData(billingProject, workspaceName, "entities", TestData.SingleParticipant.participantEntity)

      signIn(uiUser)
      val workspaceMethodConfigPage = new WorkspaceMethodConfigPage(billingProject, workspaceName)
      workspaceMethodConfigPage.open
      val methodConfigDetailsPage = workspaceMethodConfigPage.importMethodConfigFromRepo(TestData.SimpleMethod.namespace,
        TestData.SimpleMethod.name, TestData.SimpleMethod.snapshotId, methodName, Some("participant_set"))
      methodConfigDetailsPage.editMethodConfig(inputs = Some(TestData.SimpleMethod.inputs))
      val launchModal = methodConfigDetailsPage.openlaunchModal()
      assert(launchModal.verifyNoDefaultEntityMessage())
      launchModal.closeModal()
    }
  }

  "launch modal with workflows warning" in withWebDriver { implicit driver =>
    withWorkspace(billingProject, "TestSpec_FireCloud_launch_modal_with_workflows_warning") { workspaceName =>

      api.importMetaData(billingProject, workspaceName, "entities", TestData.SingleParticipant.participantEntity)
      api.importMetaData(billingProject, workspaceName, "entities", TestData.HundredAndOneSampleSet.samples)
      api.importMetaData(billingProject, workspaceName, "entities", TestData.HundredAndOneSampleSet.sampleSetCreation)
      api.importMetaData(billingProject, workspaceName, "entities", TestData.HundredAndOneSampleSet.sampleSetMembership)

      signIn(uiUser)
      val workspaceMethodConfigPage = new WorkspaceMethodConfigPage(billingProject, workspaceName)
      workspaceMethodConfigPage.open
      val methodConfigDetailsPage = workspaceMethodConfigPage.importMethodConfigFromRepo(TestData.SimpleMethod.namespace,
        TestData.SimpleMethod.name, TestData.SimpleMethod.snapshotId, methodName, Some("sample"))
      methodConfigDetailsPage.editMethodConfig(inputs = Some(TestData.SimpleMethod.inputs))

      val launchModal = methodConfigDetailsPage.openlaunchModal()
      launchModal.filterRootEntityType("sample_set")
      launchModal.searchAndSelectEntity(TestData.HundredAndOneSampleSet.entityId)
      assert(launchModal.verifyWorkflowsWarning())
      launchModal.closeModal()
    }
  }

  "launch workflow with wrong root entity" in withWebDriver { implicit driver =>
    withWorkspace(billingProject, "TestSpec_FireCloud_launch_workflow_with_wrong_root_entity") { workspaceName =>
      api.importMetaData(billingProject, workspaceName, "entities", TestData.SingleParticipant.participantEntity)

      signIn(uiUser)
      val workspaceListPage = new WorkspaceListPage
      val workspaceMethodConfigPage = new WorkspaceMethodConfigPage(billingProject, workspaceName)
      workspaceMethodConfigPage.open
      val methodConfigDetailsPage = workspaceMethodConfigPage.importMethodConfigFromRepo(TestData.SimpleMethod.namespace,
        TestData.SimpleMethod.name, TestData.SimpleMethod.snapshotId, methodName, Some("sample"))
      methodConfigDetailsPage.editMethodConfig(inputs = Some(TestData.SimpleMethod.inputs))

      val launchModal = methodConfigDetailsPage.openlaunchModal()
      launchModal.filterRootEntityType("participant")
      launchModal.searchAndSelectEntity(TestData.SingleParticipant.entityId)
      launchModal.clickLaunchButton()
      assert(launchModal.verifyWrongEntityError(wrongRootEntityErrorText))
      launchModal.closeModal()
    }
  }

  "launch workflow on set without expression" in withWebDriver { implicit driver =>
    withWorkspace(billingProject, "TestSpec_FireCloud_launch_workflow_on_set_without_expression") { workspaceName =>

      api.importMetaData(billingProject, workspaceName, "entities", TestData.SingleParticipant.participantEntity)
      api.importMetaData(billingProject, workspaceName, "entities", TestData.HundredAndOneSampleSet.samples)
      api.importMetaData(billingProject, workspaceName, "entities", TestData.HundredAndOneSampleSet.sampleSetCreation)
      api.importMetaData(billingProject, workspaceName, "entities", TestData.HundredAndOneSampleSet.sampleSetMembership)

      signIn(uiUser)
      val workspaceMethodConfigPage = new WorkspaceMethodConfigPage(billingProject, workspaceName).open
      val methodConfigDetailsPage = workspaceMethodConfigPage.importMethodConfigFromRepo(TestData.SimpleMethod.namespace,
        TestData.SimpleMethod.name, TestData.SimpleMethod.snapshotId, methodName, Some("sample"))
      methodConfigDetailsPage.editMethodConfig(inputs = Some(TestData.SimpleMethod.inputs))

      val launchModal = methodConfigDetailsPage.openlaunchModal()
      launchModal.filterRootEntityType("sample_set")
      launchModal.searchAndSelectEntity(TestData.HundredAndOneSampleSet.entityId)
      launchModal.clickLaunchButton()
      assert(launchModal.verifyWrongEntityError(noExpressionErrorText))
      launchModal.closeModal()
    }
  }

  "launch workflow with input not defined" in withWebDriver { implicit driver =>
    withWorkspace(billingProject, "TestSpec_FireCloud_launch_workflow_input_not_defined") { workspaceName =>
      api.importMetaData(billingProject, workspaceName, "entities", TestData.SingleParticipant.participantEntity)

      signIn(uiUser)
      val workspaceMethodConfigPage = new WorkspaceMethodConfigPage(billingProject, workspaceName)
      workspaceMethodConfigPage.open
      val methodConfigDetailsPage = workspaceMethodConfigPage.importMethodConfigFromRepo(TestData.InputRequiredMethodConfig.namespace,
        TestData.InputRequiredMethodConfig.name, TestData.InputRequiredMethodConfig.snapshotId, methodConfigName, Some(TestData.InputRequiredMethodConfig.rootEntityType))

      val launchModal = methodConfigDetailsPage.openlaunchModal()
      launchModal.filterRootEntityType(TestData.InputRequiredMethodConfig.rootEntityType)
      launchModal.searchAndSelectEntity(TestData.SingleParticipant.entityId)
      launchModal.clickLaunchButton()
      assert(launchModal.verifyMissingInputsError(missingInputsErrorText))
      launchModal.closeModal()
    }
  }

  // This testcase requires pulling in new code from develop branch
  "import a method config from a workspace" in withWebDriver { implicit driver =>

  }

  "import a method config into a workspace from the method repo" in withWebDriver { implicit driver =>
    withWorkspace(billingProject, "TestSpec_FireCloud_import_method_config_from_workspace") { workspaceName =>
      api.importMetaData(billingProject, workspaceName, "entities", TestData.SingleParticipant.participantEntity)

      signIn(uiUser)
      val workspaceMethodConfigPage = new WorkspaceMethodConfigPage(billingProject, workspaceName).open
      val methodConfigDetailsPage = workspaceMethodConfigPage.importMethodConfigFromRepo(TestData.SimpleMethodConfig.namespace,
        TestData.SimpleMethodConfig.name, TestData.SimpleMethodConfig.snapshotId, methodConfigName)
      //    methodConfigDetailsPage.editMethodConfig(inputs = Some(TestData.SimpleMethodConfig.inputs)) // not needed for config

      assert(methodConfigDetailsPage.isLoaded)
    }
  }

  "import a method into a workspace from the method repo" in withWebDriver { implicit driver =>
    withWorkspace(billingProject, "TestSpec_FireCloud_import_method_from_workspace") { workspaceName =>
      api.importMetaData(billingProject, workspaceName, "entities", TestData.SingleParticipant.participantEntity)

      signIn(uiUser)
      val workspaceMethodConfigPage = new WorkspaceMethodConfigPage(billingProject, workspaceName).open
      val methodConfigDetailsPage = workspaceMethodConfigPage.importMethodConfigFromRepo(TestData.SimpleMethod.namespace,
        TestData.SimpleMethod.name, TestData.SimpleMethod.snapshotId, methodName, Some(TestData.SimpleMethod.rootEntityType))
      methodConfigDetailsPage.editMethodConfig(inputs = Some(TestData.SimpleMethod.inputs))

      assert(methodConfigDetailsPage.isLoaded)
    }
  }

  "launch a method config from the method repo" in withWebDriver { implicit driver =>
    withWorkspace(billingProject, "TestSpec_FireCloud_launch_method_config_from_workspace") { workspaceName =>
      api.importMetaData(billingProject, workspaceName, "entities", TestData.SingleParticipant.participantEntity)

      signIn(uiUser)
      val workspaceMethodConfigPage = new WorkspaceMethodConfigPage(billingProject, workspaceName).open
      val methodConfigDetailsPage = workspaceMethodConfigPage.importMethodConfigFromRepo(TestData.SimpleMethodConfig.namespace,
        TestData.SimpleMethodConfig.name, TestData.SimpleMethodConfig.snapshotId, methodConfigName)
      //    methodConfigDetailsPage.editMethodConfig(inputs = Some(TestData.SimpleMethodConfig.inputs)) // not needed for config
      val submissionDetailsPage = methodConfigDetailsPage.launchAnalysis(TestData.SimpleMethodConfig.rootEntityType, TestData.SingleParticipant.entityId)

      submissionDetailsPage.waitUntilSubmissionCompletes()
      assert(submissionDetailsPage.verifyWorkflowSucceeded())
    }
  }

  "launch a method from the method repo" in withWebDriver { implicit driver =>
    withWorkspace(billingProject, "TestSpec_FireCloud_launch_method_from_workspace") { workspaceName =>
      api.importMetaData(billingProject, workspaceName, "entities", TestData.SingleParticipant.participantEntity)

      signIn(uiUser)
      val workspaceMethodConfigPage = new WorkspaceMethodConfigPage(billingProject, workspaceName).open
      val methodConfigDetailsPage = workspaceMethodConfigPage.importMethodConfigFromRepo(TestData.SimpleMethod.namespace,
        TestData.SimpleMethod.name, TestData.SimpleMethod.snapshotId, methodName, Some(TestData.SimpleMethod.rootEntityType))
      methodConfigDetailsPage.editMethodConfig(inputs = Some(TestData.SimpleMethod.inputs))
      val submissionDetailsPage = methodConfigDetailsPage.launchAnalysis(TestData.SimpleMethod.rootEntityType, TestData.SingleParticipant.entityId)

      submissionDetailsPage.waitUntilSubmissionCompletes()
      assert(submissionDetailsPage.verifyWorkflowSucceeded())
    }
  }

  "abort a workflow" in withWebDriver { implicit driver =>
    withWorkspace(billingProject, "TestSpec_FireCloud_abort_workflow") { workspaceName =>
      val shouldUseCallCaching = false
      api.importMetaData(billingProject, workspaceName, "entities", TestData.SingleParticipant.participantEntity)

      signIn(uiUser)
      // ATODO import this method via API
      val workspaceMethodConfigPage = new WorkspaceMethodConfigPage(billingProject, workspaceName).open
      val methodConfigDetailsPage = workspaceMethodConfigPage.importMethodConfigFromRepo(TestData.SimpleMethod.namespace,
        TestData.SimpleMethod.name, TestData.SimpleMethod.snapshotId, methodName, Some(TestData.SimpleMethod.rootEntityType))
      methodConfigDetailsPage.editMethodConfig(inputs = Some(TestData.SimpleMethod.inputs))
      val submissionDetailsPage = methodConfigDetailsPage.launchAnalysis(TestData.SimpleMethod.rootEntityType, TestData.SingleParticipant.entityId, "", shouldUseCallCaching)
      // ATODO start the submission via API? - reduce the amount of UI surface.
      submissionDetailsPage.abortSubmission()
      submissionDetailsPage.waitUntilSubmissionCompletes()
      assert(submissionDetailsPage.verifyWorkflowAborted())

    }
  }

  "delete a method from a workspace" in withWebDriver { implicit driver =>
    withWorkspace(billingProject, "TestSpec_FireCloud_delete_method_from_workspace") { workspaceName =>
      api.importMetaData(billingProject, workspaceName, "entities", TestData.SingleParticipant.participantEntity)

      signIn(uiUser)
      val workspaceMethodConfigPage = new WorkspaceMethodConfigPage(billingProject, workspaceName).open
      val methodConfigDetailsPage = workspaceMethodConfigPage.importMethodConfigFromRepo(TestData.SimpleMethod.namespace,
        TestData.SimpleMethod.name, TestData.SimpleMethod.snapshotId, methodName, Some(TestData.SimpleMethod.rootEntityType))
      methodConfigDetailsPage.editMethodConfig(inputs = Some(TestData.SimpleMethod.inputs))
      methodConfigDetailsPage.deleteMethodConfig()

      workspaceMethodConfigPage.filter(methodConfigName)
      assert(find(methodConfigName).size == 0)
    }
  }


  // negative tests

}
