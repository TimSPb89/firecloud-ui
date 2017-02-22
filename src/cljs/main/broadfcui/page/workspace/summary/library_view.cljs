(ns broadfcui.page.workspace.summary.library-view
  (:require
    [dmohs.react :as react]
    [broadfcui.common :as common]
    [broadfcui.common.modal :as modal]
    [broadfcui.common.style :as style]
    [broadfcui.page.workspace.summary.catalog.wizard :refer [CatalogWizard]]
    [broadfcui.page.workspace.summary.library-utils :as library-utils]
    [broadfcui.utils :as utils]
    ))




(react/defc LibraryView
  {:render
   (fn [{:keys [props state]}]
     (let [{:keys [library-attributes library-schema]} props
           wizard-properties (select-keys props [:library-schema :workspace :workspace-id :request-refresh])]
       [:div {}
        (style/create-section-header
          [:div {}
           [:span {} "Dataset Attributes"]
           (when (:can-edit? props)
             (style/create-link {:style {:fontSize "0.8em" :fontWeight "normal" :marginLeft "1em"}
                                 :text "Edit..."
                                 :onClick #(modal/push-modal [CatalogWizard wizard-properties])}))])
        (style/create-paragraph
          [:div {}
           (map (partial library-utils/render-property library-schema library-attributes) (-> library-schema :display :primary))
           [:div {}
            (when (:expanded? @state)
              [:div {}
               (map (partial library-utils/render-property library-schema library-attributes) (-> library-schema :display :secondary))
               (library-utils/render-consent-codes library-schema library-attributes)])
            [:div {:style {:marginTop "0.5em"}}
             (style/create-link {:text (if (:expanded? @state) "Collapse" "See more attributes")
                                 :onClick #(swap! state update :expanded? not)})]]])]))})
