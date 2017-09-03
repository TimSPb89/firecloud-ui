(ns broadfcui.page.method-repo.method.details
  (:require
   [dmohs.react :as react]
   [broadfcui.common :as common]
   [broadfcui.common.components :as comps]
   [broadfcui.common.style :as style]
   [broadfcui.components.tab-bar :as tab-bar]
   [broadfcui.config :as config]
   [broadfcui.endpoints :as endpoints]
   [broadfcui.page.method-repo.method.summary :refer [Summary]]
   [broadfcui.page.method-repo.method.wdl :refer [WDLViewer]]
   [broadfcui.page.method-repo.method.configs :refer [Configs]]
   [broadfcui.nav :as nav]
   [broadfcui.net :as net]
   [broadfcui.utils :as utils]
   ))


(def ^:private SUMMARY "Summary")
(def ^:private WDL "WDL")
(def ^:private CONFIGS "Configurations")

(react/defc- MethodDetails
  {:get-initial-state
   (fn [{:keys [props]}]
     {:snapshot-id (:snapshot-id props)})
   :render
   (fn [{:keys [props state refs this]}]
     (let [{:keys [method-id]} props
           {:keys [method method-error snapshot-id selected-snapshot]} @state
           active-tab (:tab-name props)
           request-refresh #(this :-refresh-method)
           refresh-tab #((@refs %) :refresh)]
       [:div {}
        [:div {:style {:marginTop "1.5rem" :padding "0 1.5rem" :display "flex"}}
         (tab-bar/render-title
          "METHOD"
          [:span {}
           [:span {:data-test-id (config/when-debug "header-namespace")} (:namespace method-id)]
           "/"
           [:span {:data-test-id (config/when-debug "header-name")} (:name method-id)]])
         [:div {:style {:paddingLeft "2rem"}} (this :-render-snapshot-selector)]]
        (tab-bar/create-bar (merge {:tabs [[SUMMARY :method-summary]
                                           [WDL :method-wdl]
                                           [CONFIGS :method-configs]]
                                    :context-id (merge method-id (utils/restructure snapshot-id))
                                    :active-tab (or active-tab SUMMARY)}
                                   (utils/restructure request-refresh refresh-tab)))
        [:div {:style {:marginTop "2rem"}}
         (if method-error
           [:div {:style {:textAlign "center" :color (:exception-state style/colors)}
                  :data-test-id (config/when-debug "method-details-error")}
            "Error loading method: " method-error]
           (if-not selected-snapshot
             [:div {:style {:textAlign "center" :padding "1rem"}}
              [comps/Spinner {:text "Loading method..."}]]
             (condp = active-tab
               nil (react/create-element
                    [Summary
                     (merge {:key method-id :ref SUMMARY}
                            (utils/restructure method-id method request-refresh))])
               WDL (react/create-element
                    [WDLViewer
                     (merge {:ref WDL :wdl (:payload selected-snapshot)}
                            (utils/restructure request-refresh))])
               CONFIGS (react/create-element
                        [Configs
                         (merge {:ref CONFIGS
                                 :on-submission-success #(nav/go-to-path :method-submission method-id %)}
                                (utils/restructure method-id method request-refresh)
                                (select-keys props [:config-id]))]))))]]))
   :component-will-mount
   (fn [{:keys [this]}]
     (this :-refresh-method))
   ;:component-will-receive-props
   ;(fn [{:keys [this after-update]}]
   ;  (after-update this :-refresh-method))
   :-render-snapshot-selector
   (fn [{:keys [state this]}]
     (let [{:keys [method snapshot-id]} @state
           selected-snapshot-id (or
                                 snapshot-id
                                 (:snapshot-id (last method)))]
       (common/render-dropdown-menu
        {:label (tab-bar/render-title
                 "SNAPSHOT"
                 [:div {:style {:display "flex" :alignItems "center"}
                        :data-test-id (config/when-debug "snapshot-dropdown")}
                  [:span {} (if (:method @state) selected-snapshot-id "Loading...")]
                  [:span {:style {:marginLeft "0.25rem" :fontSize 8 :lineHeight "inherit"}} "▼"]])
         :width :auto
         :button-style {}
         :items (vec (map
                      (fn [{:keys [snapshot-id]}]
                        {:text snapshot-id
                         :dismiss #(this :-refresh-snapshot snapshot-id)})
                      method))})))
   :-refresh-method
   (fn [{:keys [props state this]}]
     (endpoints/call-ajax-orch
      {:endpoint (endpoints/get-method-snapshots (:method-id props))
       :on-done (net/handle-ajax-response
                 (fn [{:keys [success? parsed-response]}]
                   (if success?
                     (do
                       (swap! state assoc :method parsed-response)
                       (when-not (and (:snapshot-id @state) (:selected-snapshot @state))
                         (this :-refresh-snapshot (or (:snapshot-id @state) (:snapshotId (last parsed-response))))))
                     (swap! state assoc :method-error (:message parsed-response)))))}))
   :-refresh-snapshot
   (fn [{:keys [state props]} snapshot-id]
     (let [{:keys [namespace name]} (:method-id props)]
       (endpoints/call-ajax-orch
        {:endpoint (endpoints/get-agora-method namespace name snapshot-id)
         :on-done (net/handle-ajax-response
                   (fn [{:keys [success? parsed-response]}]
                     (if success?
                       (do
                         (swap! state assoc :selected-snapshot parsed-response :snapshot-id snapshot-id)
                         (nav/push-history-item :method-summary nil namespace name snapshot-id))
                       (swap! state assoc :method-error (:message parsed-response)))))})))})

(defn- method-path [{:keys [namespace name snapshot-id] :as foo}]
  (utils/log foo)
  (str "methods/" namespace "/" name "/" snapshot-id))

(defn add-nav-paths []
  (nav/defpath
   :method-loader
   {:component MethodDetails
    :regex #"methods/([^/]+)/([^/]+)/$"
    :make-props (fn [namespace name]
                  {:method-id (utils/restructure namespace name)})
    :make-path method-path})
  (nav/defpath
   :method-summary
   {:component MethodDetails
    :regex #"methods/([^/]+)/([^/]+)/(\d+)"
    :make-props (fn [namespace name snapshot-id]
                  {:method-id (utils/restructure namespace name)
                   :snapshot-id snapshot-id})
    :make-path method-path})
  (nav/defpath
   :method-wdl
   {:component MethodDetails
    :regex #"methods/([^/]+)/([^/]+)/(\d+)/wdl"
    :make-props (fn [namespace name snapshot-id]
                  {:method-id (utils/restructure namespace name)
                   :snapshot-id snapshot-id
                   :tab-name "WDL"})
    :make-path (fn [method-id]
                 (str (method-path method-id) "/wdl"))})
  (nav/defpath
   :method-configs
   {:component MethodDetails
    :regex #"methods/([^/]+)/([^/]+)/(\d+)/configs"
    :make-props (fn [namespace name snapshot-id]
                  {:method-id (utils/restructure namespace name)
                   :snapshot-id snapshot-id
                   :tab-name "Configurations"})
    :make-path (fn [method-id]
                 (str (method-path method-id) "/configs"))})
  (nav/defpath
   :method-config
   {:component MethodDetails
    :regex #"methods/([^/]+)/([^/]+)/(\d+)/configs/([^/]+)/([^/]+)"
    :make-props (fn [namespace name snapshot-id config-ns config-name]
                  {:method-id (utils/restructure namespace name)
                   :snapshot-id snapshot-id
                   :tab-name "Configurations"
                   :config-id {:namespace config-ns :name config-name}})
    :make-path (fn [method-id config-id]
                 (str (method-path method-id) "/configs/"
                      (:namespace config-id) "/" (:name config-id)))}))
