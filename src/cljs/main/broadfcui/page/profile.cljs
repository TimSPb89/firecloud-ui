(ns broadfcui.page.profile
  (:require
    clojure.string
    [dmohs.react :as react]
    [broadfcui.common :as common]
    [broadfcui.common.components :as components]
    [broadfcui.common.icons :as icons]
    [broadfcui.common.input :as input]
    [broadfcui.common.style :as style]
    [broadfcui.config :as config]
    [broadfcui.endpoints :as endpoints]
    [broadfcui.nav :as nav]
    [broadfcui.utils :as utils]
    ))


(defn get-nih-link-href []
  (str (get @config/config "shibbolethUrlRoot")
       "/link-nih-account?redirect-url="
       (js/encodeURIComponent
        (let [loc (.-location js/window)]
          (str (.-protocol loc) "//" (.-host loc) "/#profile/nih-username-token={token}")))))

(defn is-within-last-24-hours? [epoch-time]
  (< (.now js/Date) (+ epoch-time 1000 * 60 * 60 * 24)))

(react/defc NihLink
  {:render
   (fn [{:keys [state]}]
     (let [status (:nih-status @state)
           username (get status "linkedNihUsername")
           expire-time (* (get status "linkExpireTime") 1000)
           expired? (< expire-time (.now js/Date))
           expiring-soon? (< expire-time (utils/_24-hours-from-now-ms))
           authorized? (get status "isDbgapAuthorized")
           linked-recently? (is-within-last-24-hours? (* (get status "lastLinkTime") 1000))
           pending? (and username (not authorized?) (not expired?) linked-recently?)]
       [:div {}
        [:h3 {} "Linked NIH Account"]
        (cond
          (:error-message @state) (style/create-server-error-message (:error-message @state))
          (:pending-nih-username-token @state)
          [components/Spinner {:ref "pending-spinner" :text "Linking NIH account..."}]
          (nil? username)
          [:a {:href (get-nih-link-href)}
           "Log-In to NIH to link your account"]
          :else
          [:div {}
           [:div {:style {:display "flex"}}
            [:div {:style {:flex "0 0 20ex"}} "eRA Commons / NIH Username:"]
            [:div {:style {:flex "0 0 auto"}} username]]
           [:div {:style {:display "flex" :marginTop "1em"}}
            [:div {:style {:flex "0 0 20ex"}} "Link Expiration:"]
            [:div {:style {:flex "0 0 auto"}}
             (if expired?
               [:span {:style {:color "red"}} "Expired"]
               [:span {:style {:color (when expiring-soon? "red")}} (common/format-date expire-time)])
             [:br]
             [:a {:href (get-nih-link-href)}
              "Log-In to NIH to re-link your account"]]]
           [:div {:style {:display "flex" :marginTop "1em"}}
            [:div {:style {:flex "0 0 20ex"}} "dbGaP Authorization:"]
            [:div {:style {:flex "0 0 auto"}}
             (cond
               authorized? [:span {:style {:color (:success-state style/colors)}} "Authorized"]
               pending? [:span {:style {:color (:success-state style/colors)}}
                         "Your link was successful; you will be granted access shortly."]
               :else [:span {:style {:color (:text-light style/colors)}} "Not Authorized"])]]])]))
   :component-did-mount
   (fn [{:keys [this props state after-update]}]
     (let [nav-context (nav/parse-segment (:parent-nav-context props))
           segment (:segment nav-context)]
       (if-not (clojure.string/blank? segment)
         (do
           (assert (re-find #"^nih-username-token=" segment) "Unexpected URL hash")
           (let [[_ token] (clojure.string/split segment #"=")]
             (swap! state assoc :pending-nih-username-token token)
             (after-update #(react/call :link-nih-account this token))
             ;; Navigate to the parent (this page without the token), but replace the location so
             ;; the back button doesn't take the user back to the token.
             (.replace (.-location js/window)
                       (str "#" (nav/create-hash (:parent-nav-context props))))))
         (react/call :load-nih-status this))))
   :component-did-update
   (fn [{:keys [refs]}]
     (when (@refs "pending-spinner")
       (common/scroll-to-center (-> (@refs "pending-spinner") react/find-dom-node))))
   :load-nih-status
   (fn [{:keys [state]}]
     (endpoints/profile-get-nih-status
      (fn [{:keys [success? status-code status-text get-parsed-response]}]
        (cond
          success? (swap! state assoc :nih-status (get-parsed-response false))
          (= status-code 404) (swap! state assoc :nih-status :none)
          :else
          (swap! state assoc :error-message status-text)))))
   :link-nih-account
   (fn [{:keys [this state]} token]
     (endpoints/profile-link-nih-account
      token
      (fn [{:keys [success?]}]
        (if success?
          (do (swap! state dissoc :pending-nih-username-token :nih-status)
              (react/call :load-nih-status this))
          (swap! state assoc :error-message "Failed to link NIH account")))))})


(react/defc Form
  {:get-field-keys
   (fn []
     (list :firstName :lastName :title :contactEmail :institute :institutionalProgram :programLocationCity
           :programLocationState :programLocationCountry :pi))
   :get-values
   (fn [{:keys [state]}]
     (reduce-kv (fn [r k v] (assoc r k (clojure.string/trim v))) {} (:values @state)))
   :validation-errors
   (fn [{:keys [refs this]}]
     (apply input/validate refs (map name (react/call :get-field-keys this))))
   :render
   (fn [{:keys [this props state]}]
     (cond (:error-message @state) (style/create-server-error-message (:error-message @state))
           (:values @state)
           [:div {}
            [:div {:style {:fontWeight "bold" :margin "1em 0 1em 0"}} "* - required fields"]
            (react/call :render-nested-field this :firstName "First Name" true)
            (react/call :render-nested-field this :lastName "Last Name" true)
            (react/call :render-field this :title "Title" true)
            (react/call :render-field this :contactEmail "Contact Email (to receive FireCloud notifications)" false true)
            (react/call :render-nested-field this :institute "Institute" true)
            (react/call :render-nested-field this :institutionalProgram "Institutional Program" true)
            (common/clear-both)
            [:div {}
             [:div {:style {:marginTop "0.5em" :fontSize "88%"}} "*Non-Profit Status"]
             [:div {:style {:fontSize "88%"}}
              (react/call :render-radio-field this :nonProfitStatus "Profit")
              (react/call :render-radio-field this :nonProfitStatus "Non-Profit")]]
            (react/call :render-field this :pi "Principal Investigator/Program Lead" true)
            [:div {}
             [:div {:style {:fontSize "88%"}} "Program Location:"]
             [:div {}
              (react/call :render-nested-field this :programLocationCity "City" true)
              (react/call :render-nested-field this :programLocationState "State/Province" true)
              (react/call :render-nested-field this :programLocationCountry "Country" true)]]
            (common/clear-both)
            (when-not (:new-registration? props)
              [:div {} [NihLink {:parent-nav-context (:parent-nav-context props)}]])]
           :else [components/Spinner {:text "Loading User Profile..."}]))
   :render-radio-field
   (fn [{:keys [state]} key value]
     [:div {:style {:float "left" :margin "0 1em 0.5em 0" :padding "0.5em 0"}}
      [:label {}
       [:input {:type "radio" :value value :name key
                :checked (= (get-in @state [:values key]) value)
                :onChange #(swap! state assoc-in [:values key] value)}]
       value]])
   :render-nested-field
   (fn [{:keys [state]} key label required]
     [:div {:style {:float "left" :marginBottom "0.5em" :marginTop "0.5em"}}
      [:label {}
       [:div {:style {:fontSize "88%"}} (str (when required "*") label ":")]]
      [input/TextField {:style {:marginRight "1em" :width 200}
                        :defaultValue (get-in @state [:values key])
                        :ref (name key) :placeholder (get-in @state [:values key])
                        :predicates [(when required (input/nonempty label))]
                        :onChange #(swap! state assoc-in [:values key] (-> % .-target .-value))}]])
   :render-field
   (fn [{:keys [state]} key label required valid-email-or-empty]
     [:div {:style {:clear "both" :margin "0.5em 0"}}
      [:label {}
       (style/create-form-label (str (when required "*") label ":"))
       [input/TextField {:style {:width 200}
                         :defaultValue (get-in @state [:values key])
                         :ref (name key) :placeholder (get-in @state [:values key])
                         :predicates [(when required (input/nonempty label))
                                      (when valid-email-or-empty (input/valid-email-or-empty label))]
                         :onChange #(swap! state assoc-in [:values key] (-> % .-target .-value))}]]])
   :component-did-mount
   (fn [{:keys [state]}]
     (endpoints/profile-get
      (fn [{:keys [success? status-text get-parsed-response]}]
        (if success?
          (let [parsed (get-parsed-response false)]
            (swap! state assoc :values (common/parse-profile parsed)))
          (swap! state assoc :error-message status-text)))))})


(react/defc Page
  {:render
   (fn [{:keys [this props state]}]
     (let [new? (:new-registration? props)
           update? (:update-registration? props)]
       [:div {:style {:margin "1em 2em"}}
        [:h2 {} (cond new? "New User Registration"
                      update? "Update Registration"
                      :else "Profile")]
        [:div {}
         [Form {:ref "form" :parent-nav-context (:nav-context props)
                :new-registration? (:new-registration? props)}]]
        [:div {:style {:marginTop "2em"}}
         (when (:server-error @state)
           [:div {:style {:marginBottom "1em"}}
            [components/ErrorViewer {:error (:server-error @state)}]])
         (when (:validation-errors @state)
           [:div {:style {:marginBottom "1em"}}
            (style/create-flexbox {}
              [:span {:style {:paddingRight "1ex"}}
               (icons/icon {:style {:color (:exception-state style/colors)}}
                           :warning-triangle)]
              "Validation Errors:")
            [:ul {}
             (map (fn [e] [:li {} e]) (:validation-errors @state))]])
         (cond
           (:done? @state)
           [:div {:style {:color (:success-state style/colors)}} "Profile saved!"]
           (:in-progress? @state)
           [components/Spinner {:text "Saving..."}]
           :else
           [components/Button {:text (if new? "Register" "Save Profile")
                               :onClick #(react/call :save this)}])]]))
   :save
   (fn [{:keys [props state refs]}]
     (swap! state (fn [s] (assoc (dissoc s :server-error :validation-errors) :in-progress? true)))
     (let [values (react/call :get-values (@refs "form"))
           validation-errors (react/call :validation-errors (@refs "form"))]
       (cond
         (nil? validation-errors)
         (endpoints/profile-set
          values
          (fn [{:keys [success? get-parsed-response]}]
            (swap! state (fn [s]
                           (let [new-state (dissoc s :in-progress? :validation-errors)]
                             (if-not success?
                               (assoc new-state :server-error (get-parsed-response false))
                               (let [on-done (or (:on-done props) #(swap! state dissoc :done?))]
                                 (js/setTimeout on-done 2000)
                                 (assoc new-state :done? true))))))))
         :else
         (swap! state (fn [s]
                        (let [new-state (dissoc s :in-progress? :done?)]
                          (assoc new-state :validation-errors validation-errors)))))))})

(defn render [props]
  (react/create-element Page props))