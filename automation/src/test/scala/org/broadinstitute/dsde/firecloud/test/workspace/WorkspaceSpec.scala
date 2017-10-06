package org.broadinstitute.dsde.firecloud.test.workspace

import java.util.UUID

import org.broadinstitute.dsde.firecloud.api.{AclEntry, WorkspaceAccessLevel}
import org.broadinstitute.dsde.firecloud.config.{AuthToken, AuthTokens, Config}
import org.broadinstitute.dsde.firecloud.fixture.MethodData.{SimpleMethod, SimpleMethodConfig}
import org.broadinstitute.dsde.firecloud.fixture.{MethodData, MethodFixtures, TestData, WorkspaceFixtures}
import org.broadinstitute.dsde.firecloud.page.workspaces.methodconfigs.WorkspaceMethodConfigListPage
import org.broadinstitute.dsde.firecloud.page.workspaces.summary.WorkspaceSummaryPage
import org.broadinstitute.dsde.firecloud.test.{CleanUp, WebBrowserSpec}
import org.scalatest._

class WorkspaceSpec extends FreeSpec with WebBrowserSpec with WorkspaceFixtures with MethodFixtures
  with CleanUp with Matchers {

  val methodConfigName: String = MethodData.SimpleMethodConfig.configName + "_" + UUID.randomUUID().toString
  implicit val authToken: AuthToken = AuthTokens.harry
  val billingProject: String = Config.Projects.default

  val testAttributes = Map("A-key" -> "A value", "B-key" -> "B value", "C-key" -> "C value")

  "A user" - {
    "with a billing project" - {
      "should be able to create a workspace" in withWebDriver { implicit driver =>
        val listPage = signIn(Config.Users.harry)

        val workspaceName = "WorkspaceSpec_create_" + randomUuid
        register cleanUp api.workspaces.delete(billingProject, workspaceName)
        val detailPage = listPage.createWorkspace(billingProject, workspaceName)

        detailPage.validateWorkspace shouldEqual true

        listPage.open
        listPage.hasWorkspace(billingProject, workspaceName) shouldBe true
      }

      "should be able to clone a workspace" in withWebDriver { implicit driver =>
        withWorkspace(billingProject, "WorkspaceSpec_to_be_cloned") { workspaceName =>
          val listPage = signIn(Config.Users.harry)

          val workspaceNameCloned = "WorkspaceSpec_clone_" + randomUuid
          val workspaceSummaryPage = new WorkspaceSummaryPage(billingProject, workspaceName).open
          register cleanUp api.workspaces.delete(billingProject, workspaceNameCloned)
          workspaceSummaryPage.cloneWorkspace(billingProject, workspaceNameCloned)

          listPage.open
          listPage.hasWorkspace(billingProject, workspaceNameCloned) shouldBe true
        }
      }
    }

    "who owns a workspace" - {
      "should be able to delete the workspace" in withWebDriver { implicit driver =>
        withWorkspace(billingProject, "WorkspaceSpec_delete") { workspaceName =>
          val listPage = signIn(Config.Users.harry)

          val detailPage = listPage.enterWorkspace(billingProject, workspaceName)
          detailPage.deleteWorkspace()
          listPage.validateLocation()
          listPage.hasWorkspace(billingProject, workspaceName) shouldBe false
        }
      }

      "should be able to share the workspace" in withWebDriver { implicit driver =>
        withWorkspace(billingProject, "WorkspaceSpec_share") { workspaceName =>
          val listPage = signIn(Config.Users.harry)

          val detailPage = listPage.enterWorkspace(billingProject, workspaceName)
          detailPage.share(Config.Users.ron.email, "READER")
          detailPage.signOut()
          val listPage2 = signIn(Config.Users.ron)
          val detailPage2 = listPage2.enterWorkspace(billingProject, workspaceName)
          detailPage2.ui.readAccessLevel() shouldBe WorkspaceAccessLevel.Reader
        }
      }

      "should be able to set can share permissions for other (non-owner) users" in withWebDriver {implicit driver =>
        withWorkspace(billingProject, "WorkspaceSpec_share") {workspaceName =>
          val listPage = signIn(Config.Users.harry)

          val detailPage = listPage.enterWorkspace(billingProject, workspaceName)
          detailPage.share(Config.Users.ron.email, "READER", true)
          detailPage.signOut()
          val listPage2 = signIn(Config.Users.ron)
          val detailPage2 = listPage2.enterWorkspace(billingProject, workspaceName)
          detailPage2.ui.hasShareButton shouldBe true
        }
      }

      "should be able to set can compute permissions for users that are writers" in withWebDriver {implicit driver =>
        withWorkspace(billingProject, "WorkspaceSpec_share") { workspaceName =>
          withConfigForMethodInWorkspace("MethodinWorkspaceSpec", billingProject, workspaceName) { configName =>
            api.methodConfigurations.setMethodConfigPermission("MethodinWorkspaceSpec", SimpleMethod.methodName, 1, Config.Users.ron.email, "OWNER")
            val listPage = signIn(Config.Users.harry)
            val detailPage = listPage.enterWorkspace(billingProject, workspaceName)
            val aclEditor = detailPage.openShareDialog(Config.Users.ron.email, "WRITER")
            aclEditor.canComputeEnabled shouldBe true
            aclEditor.canComputeChecked shouldBe false
          }
        }
      }

      "should not be able to set/change can compute permissions for other owners" in withWebDriver {implicit driver =>
        withWorkspace(billingProject, "WorkspaceSpec_share") { workspaceName =>
          api.importMetaData(billingProject, workspaceName, "entities", TestData.SingleParticipant.participantEntity)
          api.methodConfigurations.createMethodConfigInWorkspace(billingProject, workspaceName, 1, SimpleMethod.methodNamespace, methodConfigName, 1, SimpleMethodConfig.configNamespace, s"$methodConfigName Config",
            SimpleMethodConfig.inputs, SimpleMethodConfig.outputs, "participant")
          val listPage = signIn(Config.Users.harry)
          val detailPage = listPage.enterWorkspace(billingProject, workspaceName)
          val aclEditor = detailPage.openShareDialog(Config.Users.ron.email, "OWNER")
          aclEditor.canComputeEnabled shouldBe false
          aclEditor.canComputeChecked shouldBe true
        }
      }
      //reader permissions should always be false
      "should not be able to set/change compute permissions for readers" in withWebDriver { implicit driver =>
        withWorkspace(billingProject, "WorkspaceSpec_share") { workspaceName =>
          api.importMetaData(billingProject, workspaceName, "entities", TestData.SingleParticipant.participantEntity)
          api.methodConfigurations.createMethodConfigInWorkspace(billingProject, workspaceName, 1, SimpleMethod.methodNamespace, methodConfigName, 1, SimpleMethodConfig.configNamespace, s"$methodConfigName Config",
            SimpleMethodConfig.inputs, SimpleMethodConfig.outputs, "participant")
          val listPage = signIn(Config.Users.harry)
          val detailPage = listPage.enterWorkspace(billingProject, workspaceName)
          val aclEditor = detailPage.openShareDialog(Config.Users.ron.email, "READER")
          aclEditor.canComputeEnabled shouldBe false
          aclEditor.canComputeChecked shouldBe false
        }
      }

      "should be able to enter workspace attributes" in withWebDriver { implicit driver =>
        withWorkspace(billingProject, "WorkspaceSpec_add_ws_attrs") { workspaceName =>
          val listPage = signIn(Config.Users.harry)
          val detailPage = listPage.enterWorkspace(billingProject, workspaceName)

          detailPage.ui.beginEditing()
          detailPage.ui.addWorkspaceAttribute("a", "X")
          detailPage.ui.addWorkspaceAttribute("b", "Y")
          detailPage.ui.addWorkspaceAttribute("c", "Z")
          detailPage.ui.save()

          // TODO: ensure sort, for now it's default sorted by key, ascending
          detailPage.ui.readWorkspaceTable shouldBe List(List("a", "X"), List("b", "Y"), List("c", "Z"))
        }
      }

      // This table is notorious for getting out of sync
      "should be able to correctly delete workspace attributes" - {
        "from the top" in withWebDriver { implicit driver =>
          withWorkspace(billingProject, "WorkspaceSpec_del_ws_attrs", attributes = Some(testAttributes)) { workspaceName =>
            val listPage = signIn(Config.Users.harry)
            val detailPage = listPage.enterWorkspace(billingProject, workspaceName)

            detailPage.ui.beginEditing()
            detailPage.ui.deleteWorkspaceAttribute("A-key")
            detailPage.ui.save()

            detailPage.ui.readWorkspaceTable shouldBe List(List("B-key", "B value"), List("C-key", "C value"))
          }
        }

        "from the middle" in withWebDriver { implicit driver =>
          withWorkspace(billingProject, "WorkspaceSpec_del_ws_attrs", attributes = Some(testAttributes)) { workspaceName =>
            val listPage = signIn(Config.Users.harry)
            val detailPage = listPage.enterWorkspace(billingProject, workspaceName)

            detailPage.ui.beginEditing()
            detailPage.ui.deleteWorkspaceAttribute("B-key")
            detailPage.ui.save()

            detailPage.ui.readWorkspaceTable shouldBe List(List("A-key", "A value"), List("C-key", "C value"))
          }
        }

        "from the bottom" in withWebDriver { implicit driver =>
          withWorkspace(billingProject, "WorkspaceSpec_del_ws_attrs", attributes = Some(testAttributes)) { workspaceName =>
            val listPage = signIn(Config.Users.harry)
            val detailPage = listPage.enterWorkspace(billingProject, workspaceName)

            detailPage.ui.beginEditing()
            detailPage.ui.deleteWorkspaceAttribute("C-key")
            detailPage.ui.save()

            detailPage.ui.readWorkspaceTable shouldBe List(List("A-key", "A value"), List("B-key", "B value"))
          }
        }

        "after adding them" in withWebDriver { implicit driver =>
          withWorkspace(billingProject, "WorkspaceSpec_del_ws_attrs") { workspaceName =>
            val listPage = signIn(Config.Users.harry)
            val detailPage = listPage.enterWorkspace(billingProject, workspaceName)

            detailPage.ui.beginEditing()
            detailPage.ui.addWorkspaceAttribute("a", "W")
            detailPage.ui.addWorkspaceAttribute("b", "X")
            detailPage.ui.addWorkspaceAttribute("c", "Y")
            detailPage.ui.save()

            detailPage.ui.readWorkspaceTable shouldBe List(List("a", "W"), List("b", "X"), List("c", "Y"))

            detailPage.ui.beginEditing()
            detailPage.ui.addWorkspaceAttribute("d", "Z")
            detailPage.ui.save()

            detailPage.ui.readWorkspaceTable shouldBe List(List("a", "W"), List("b", "X"), List("c", "Y"), List("d", "Z"))

            detailPage.ui.beginEditing()
            detailPage.ui.deleteWorkspaceAttribute("c")
            detailPage.ui.save()

            detailPage.ui.readWorkspaceTable shouldBe List(List("a", "W"), List("b", "X"), List("d", "Z"))

            detailPage.ui.beginEditing()
            detailPage.ui.deleteWorkspaceAttribute("b")
            detailPage.ui.save()

            detailPage.ui.readWorkspaceTable shouldBe List(List("a", "W"), List("d", "Z"))
          }
        }
      }
    }
    "who has reader access to workspace" - {

      "should see launch analysis button disabled" in withWebDriver { implicit driver =>
        withWorkspace(billingProject, "WorkspaceSpec_readAccess", Set.empty, List(AclEntry(Config.Users.ron.email, WorkspaceAccessLevel.withName("READER")))) { workspaceName =>
          withConfigForMethodInWorkspace("MethodinWorkspaceSpec", billingProject, workspaceName) { configName =>
            val listPage = signIn(Config.Users.ron)
            val detailPage = listPage.enterWorkspace(billingProject, workspaceName)
            val methodConfigTab = detailPage.goToMethodConfigTab()
            val methodConfigDetailsPage = methodConfigTab.openMethodConfig("MethodinWorkspaceSpec", "MethodinWorkspaceSpec")
            val errorModal = methodConfigDetailsPage.clickLaunchAnalysisButtonError()
            errorModal.getErrorText shouldBe "You do not have access to run analysis.\nCancel"

          }
        }
      }

      "should see import config button disabled" in withWebDriver { implicit driver =>
        withWorkspace(billingProject, "WorkspaceSpec_readAccess", Set.empty, List(AclEntry(Config.Users.ron.email, WorkspaceAccessLevel.withName("READER")))) { workspaceName =>
          signIn(Config.Users.ron)
          val methodConfigListPage = new WorkspaceMethodConfigListPage(billingProject, workspaceName).open
          methodConfigListPage.importConfigButtonEnabled() shouldBe false
        }
      }
    }

    "who has writer access" - {
      "and does not have canCompute permission" - {
        "should see launch analysis button disabled" in withWebDriver { implicit driver =>
          withWorkspace(billingProject, "WorkspaceSpect_writerAccess") { workspaceName =>
            withConfigForMethodInWorkspace("MethodinWorkspaceSpec", billingProject, workspaceName) { configName =>
              api.methodConfigurations.setMethodConfigPermission("MethodinWorkspaceSpec", SimpleMethod.methodName, 1, Config.Users.ron.email, "OWNER")
              val listPage = signIn(Config.Users.harry)
              val detailPage = listPage.enterWorkspace(billingProject, workspaceName)
              detailPage.share(Config.Users.ron.email, "WRITER", false, false)
              detailPage.signOut()
              val listPage2 = signIn(Config.Users.ron)
              val detailPage2 = listPage2.enterWorkspace(billingProject, workspaceName)
              val methodConfigTab = detailPage2.goToMethodConfigTab()
              val methodConfigDetailsPage = methodConfigTab.openMethodConfig("MethodinWorkspaceSpec", "MethodinWorkspaceSpec")
              val errorModal = methodConfigDetailsPage.clickLaunchAnalysisButtonError()
              errorModal.getErrorText shouldBe "You do not have access to run analysis.\nCancel"
            }
          }
        }
      }
      "and does have canCompute permission" - {
        "should be able to launch analysis" ignore withWebDriver { implicit driver =>
          withWorkspace(billingProject, "WorkspaceSpec_writerAccess") { workspaceName =>
            withConfigForMethodInWorkspace("MethodinWorkspaceSpec", billingProject, workspaceName) { configName =>
              api.methodConfigurations.setMethodConfigPermission("MethodinWorkspaceSpec", SimpleMethod.methodName, 1, Config.Users.ron.email, "OWNER")
              val listPage = signIn(Config.Users.harry)
              val detailPage = listPage.enterWorkspace(billingProject, workspaceName)
              detailPage.share(Config.Users.ron.email, "WRITER", false, true)
              detailPage.signOut()
              val listPage2 = signIn(Config.Users.ron)
              val detailPage2 = listPage2.enterWorkspace(billingProject, workspaceName)
              val methodConfigTab = detailPage2.goToMethodConfigTab()
              val methodConfigDetailsPage = methodConfigTab.openMethodConfig("MethodinWorkspaceSpec", "MethodinWorkspaceSpec")
              val launchAnalysisModal = methodConfigDetailsPage.openLaunchAnalysisModal()
              launchAnalysisModal.validateLocation shouldBe true
            }
          }
        }
      }
    }

  }

  // Experimental
  "A cloned workspace" - {
    "should retain the source workspace's authorization domain" in withWebDriver { implicit driver =>
      withClonedWorkspace(Config.Projects.common, "AuthDomainSpec_share") { cloneWorkspaceName =>
        val listPage = signIn(Config.Users.harry)
        val summaryPage = listPage.enterWorkspace(Config.Projects.common, cloneWorkspaceName)
        // assert that user who cloned the workspace is the owner
      }
    }
  }
}
