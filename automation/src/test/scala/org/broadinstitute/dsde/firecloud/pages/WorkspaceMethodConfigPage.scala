package org.broadinstitute.dsde.firecloud.pages

import org.broadinstitute.dsde.firecloud.{Config, PageUtil}
import org.openqa.selenium.WebDriver
import org.scalatest.selenium.Page


class WorkspaceMethodConfigPage(namespace: String, name: String)(implicit webDriver: WebDriver) extends WorkspacePage with Page with PageUtil[WorkspaceMethodConfigPage] {

  override val url: String = s"${Config.FireCloud.baseUrl}#workspaces/$namespace/$name/method-configs"

//To-Do: Make this accept method namespace and participant
  def importMethodConfigFromRepo(methodNamespace: String, methodName: String, snapshotId: Int, methodConfigName: String, rootEntityType: String): MethodConfigDetailsPage = {
    val chooseSourceModal = gestures.clickImportConfigButton()
    chooseSourceModal.chooseConfigFromRepo(methodNamespace, methodName, snapshotId, methodConfigName, rootEntityType)
    new MethodConfigDetailsPage(namespace, name, methodNamespace, methodConfigName)
  }


  object gestures {
    private val openImportConfigModalButtonQuery: Query = testId("import-config-button")
    def clickImportConfigButton(): ImportMethodChooseSourceModel = {
      click on (await enabled openImportConfigModalButtonQuery)
      new ImportMethodChooseSourceModel()
    }
  }
}

class ImportMethodChooseSourceModel(implicit webDriver: WebDriver) extends FireCloudView {

  def chooseConfigFromRepo(methodNamespace: String, methodName: String, snapshotId: Int, methodConfigName: String, rootEntityType: String): Unit = {
    val importModel = gestures.clickChooseFromRepoButton()
    importModel.importMethodConfig(methodNamespace, methodName, snapshotId, methodConfigName, rootEntityType)
  }
  object gestures {
    private val chooseConfigFromRepoModalButtonQuery: Query = testId("import-from-repo-button")
    private val chooseConfigFromWorkspaceModalButtonQuery: Query = testId("copy-from-workspace-button")

    def clickChooseFromRepoButton(): ImportMethodConfigModal = {
      click on (await enabled chooseConfigFromRepoModalButtonQuery)
      new ImportMethodConfigModal()
    }
  }

}

/**
  * Page class for the import method config modal.
  */
class ImportMethodConfigModal(implicit webDriver: WebDriver) extends FireCloudView {

  /**
    *
    */
  def importMethodConfig(methodNamespace: String, methodName: String, snapshotId: Int, methodConfigName: String, rootEntityType: String): Unit = {
    gestures.searchMethod(methodName)
    gestures.selectMethod(methodName, snapshotId)
    gestures.fillMethodConfigName(methodConfigName)
    gestures.chooseRootEntityType(rootEntityType)
    gestures.clickimportMethodConfigButton()
  }

  object gestures {

    private val methodSearchInputQuery: Query = testId("method-repo-table-input")
    private val methodConfigNameInputQuery: Query = testId("method-config-import-name-input")
    private val importMethodConfigButtonQuery: Query = testId("import-button")
    private val rootEntityTypeSelectQuery: Query = testId("import-root-entity-type-select")

    def searchMethod(searchQuery: String): Unit = {
      await enabled methodSearchInputQuery
        searchField(methodSearchInputQuery).value = searchQuery
      pressKeys("\n")
    }

    def selectMethod(methodName: String, snapshotId: Int): Unit = {
      val methodLinkQuery: Query = testId(methodName + "_" + snapshotId)
      click on testId(methodName + "_" + snapshotId)
    }

    def fillMethodConfigName(methodConfigName: String): Unit = {
      await enabled methodConfigNameInputQuery
      textField(methodConfigNameInputQuery).value = methodConfigName
    }

    def chooseRootEntityType(rootEntityType: String) = {
      await enabled rootEntityTypeSelectQuery
      singleSel(rootEntityTypeSelectQuery).value = rootEntityType
    }

    def verifyNoDefaultEntityMessage() = {

    }

    def clickimportMethodConfigButton(): Unit = {
      click on (await enabled importMethodConfigButtonQuery)
      await enabled testId("edit-method-config-button")
    }

  }

}
