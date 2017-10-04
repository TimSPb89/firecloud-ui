package org.broadinstitute.dsde.firecloud.page.workspaces.methodconfigs

import org.broadinstitute.dsde.firecloud.FireCloudView
import org.broadinstitute.dsde.firecloud.config.Config
import org.broadinstitute.dsde.firecloud.component.{Button, Table}
import org.broadinstitute.dsde.firecloud.page.workspaces.WorkspacePage
import org.broadinstitute.dsde.firecloud.page.{OKCancelModal, PageUtil}
import org.openqa.selenium.WebDriver
import org.scalatest.selenium.Page


class WorkspaceMethodConfigListPage(namespace: String, name: String)(implicit webDriver: WebDriver)
  extends WorkspacePage(namespace, name) with Page with PageUtil[WorkspaceMethodConfigListPage] {

  override def awaitReady(): Unit = ui.methodConfigsTable.awaitReady()

  override val url: String = s"${Config.FireCloud.baseUrl}#workspaces/$namespace/$name/method-configs"


//To-Do: Make this accept method namespace and participant
  /**
    * Imports Methods and Method Configs from the Method Repo. Note that the rootEntityType is only
    * necessary for Methods, but not Method Configs
    */
  def importMethodConfigFromRepo(methodNamespace: String, methodName: String, snapshotId: Int, methodConfigName: String, rootEntityType: Option[String] = None): WorkspaceMethodConfigDetailsPage = {
    val chooseSourceModal = ui.clickImportConfigButton()
    chooseSourceModal.chooseConfigFromRepo(methodNamespace, methodName, snapshotId, methodConfigName, rootEntityType)
    new WorkspaceMethodConfigDetailsPage(namespace, name, methodNamespace, methodConfigName)
  }

  def filter(searchText: String): Unit = {
    ui.filter(searchText)
  }

  def openMethodConfig(methodNamespace: String, methodName: String): WorkspaceMethodConfigDetailsPage = {
    ui.openMethodConfig(methodName)
    new WorkspaceMethodConfigDetailsPage(namespace, name, methodNamespace, methodName)
  }
  
  trait UI extends super.UI {
    private val openImportConfigModalButton: Button = Button("import-config-button")
    private[WorkspaceMethodConfigListPage] val methodConfigsTable = Table("method-configs-table")
    private val methodConfigLinkId = "method-config-%s-link"

    def clickImportConfigButton(): ImportMethodChooseSourceModel = {
      openImportConfigModalButton.doClick()
      new ImportMethodChooseSourceModel()
    }

    def importConfigButtonEnabled(): Boolean = {
      openImportConfigModalButton.isEnabled
    }

    def filter(searchText: String): Unit = {
      methodConfigsTable.filter(searchText)
    }

    def openMethodConfig(methodName: String): Unit = {
      val linkId = methodConfigLinkId.format(methodName)
      val link = testId(linkId)
      click on (await enabled link)
    }

    def hasConfig(name: String): Boolean = {
      find(title(s"$name")).isDefined
    }

  }
  object ui extends UI
}

class ImportMethodChooseSourceModel(implicit webDriver: WebDriver) extends FireCloudView {
  override def awaitReady(): Unit = gestures.chooseConfigFromRepoModalButton.awaitVisible()

  def chooseConfigFromRepo(methodNamespace: String, methodName: String, snapshotId: Int, methodConfigName: String, rootEntityType: Option[String]): Unit = {
    val importModel = gestures.clickChooseFromRepoButton()
    importModel.importMethodConfig(methodNamespace, methodName, snapshotId, methodConfigName, rootEntityType)
  }
  object gestures {
    private[ImportMethodChooseSourceModel] val chooseConfigFromRepoModalButton = Button("import-from-repo-button")
    private val chooseConfigFromWorkspaceModalButton = Button("copy-from-workspace-button")

    def clickChooseFromRepoButton(): ImportMethodConfigModal = {
      chooseConfigFromRepoModalButton.doClick()
      new ImportMethodConfigModal()
    }
  }

}

/**
  * Page class for the import method config modal.
  */
class ImportMethodConfigModal(implicit webDriver: WebDriver) extends OKCancelModal {
  override def awaitReady(): Unit = ui.methodTable.awaitReady()

  /**
    *
    */
  def importMethodConfig(methodNamespace: String, methodName: String, snapshotId: Int, methodConfigName: String, rootEntityType: Option[String]): Unit = {
    ui.searchMethodOrConfig(methodName)
    ui.selectMethodOrConfig(methodName, snapshotId)
    ui.fillNamespace(methodNamespace)
    ui.fillMethodConfigName(methodConfigName)
    if (rootEntityType.isDefined) { ui.chooseRootEntityType(rootEntityType.get) }
    ui.clickImportMethodConfigButton()
    await spinner "Importing..."
  }

  object ui {
    private[ImportMethodConfigModal] val methodTable = Table("method-repo-table")

    private val methodNamespaceInputQuery: Query = testId("method-config-import-namespace-input")
    private val methodConfigNameInputQuery: Query = testId("method-config-import-name-input")
    private val importMethodConfigButtonQuery: Query = testId("import-button")
    private val rootEntityTypeSelectQuery: Query = testId("import-root-entity-type-select")

    def searchMethodOrConfig(searchQuery: String): Unit = {
      methodTable.filter(searchQuery)
    }

    def selectMethodOrConfig(methodName: String, snapshotId: Int): Unit = {
      val methodLinkQuery: Query = testId(methodName + "_" + snapshotId) //TODO: update the testID to have a prefix for the import method configuration modal table row.... OR a <Namespace>-<name>_<snapshotid>
      click on testId(methodName + "_" + snapshotId)
    }

    def fillNamespace(methodNamespace: String): Unit = {
      await enabled methodNamespaceInputQuery
      textField(methodNamespaceInputQuery).value = methodNamespace
    }

    def fillMethodConfigName(methodConfigName: String): Unit = {
      await enabled methodConfigNameInputQuery
      textField(methodConfigNameInputQuery).value = methodConfigName
    }

    def chooseRootEntityType(rootEntityType: String): Unit = {
      await enabled rootEntityTypeSelectQuery
      singleSel(rootEntityTypeSelectQuery).value = rootEntityType
    }


    def clickImportMethodConfigButton(): Unit = {
      click on (await enabled importMethodConfigButtonQuery)
    }

  }

}
