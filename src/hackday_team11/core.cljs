(ns ^:figwheel-always hackday-team11.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs-http.client :as http]
            [clojure.walk :refer [keywordize-keys]]
            [cljs.core.async :refer [<!]]
            [reagent.core :as reagent :refer [atom]]))

(enable-console-print!)

(println "Edits to this text should show up in your developer console.")

;; define your app data so that it doesn't get over-written on reload

(defonce app-state (atom {:logged-in? false}))

(defn json-parse [s]
  (js->clj (JSON/parse s)))

(defn prompt-message
  [message]
  [:div {:class "my-messages"}
   [:div {:class "prompt message-animation"} [:p message]]])

(defn input-element
  [id name type value]
  [:input {:id id
           :name name
           :class "form-control"
           :type type
           :required ""
           :value @value
           :on-change #(reset! value (-> % .-target .-value))}])

(defn input-and-prompt
  [label-value input-name input-type input-element-arg prompt-element required?]
  (let [input-focus (atom false)]
    (fn []
      [:div
       [:label label-value]
       (if @input-focus prompt-element [:div])
       [input-element input-name input-name input-type input-element-arg input-focus]
       (if (and required? (= "" @input-element-arg))
         [:div "Field is required!"]
         [:div])])))

(defn email-form
 [email-address-atom]
 (input-and-prompt "email"
                   "email"
                   "email"
                   email-address-atom
                   [prompt-message "What's your email?"]
                   true))

(defn password-form [password-atom]
  (input-and-prompt "salasana"
                    "password"
                    "password"
                    password-atom
                    [prompt-message "What's your password?"]
                    true))

(defn wrap-as-element-in-form
  [element]
  [:div {:class="row form-group"}
   element])

(defn render-element-with-param [id element param]
  (reagent/render-component [element param]
                            (.getElementById js/document id)))

(defn images [data]
  (let [images (remove nil? (keywordize-keys (map #(get % "image") data)))]
    [:div
      [:h3 "Suosituimmat kuvat tällä hetkellä"]
      [:ul
        (map (fn [item] 
                ^{:key item} [:li [:img {:src (:uri item)}]]) images)]]))

(defn articles [data]
  [:div
    [:h3 "Suosituimmat artikkelit tällä hetkellä"]
    [:ul
      (map (fn [item] 
            ^{:key item} [:li [:a {:href (get item "url")} (get item "lead")]]) data)]])

(defn greeting [nick]
  [:div
    [:h3 (str "Tervetuloa " nick "!")]])

(defn get-top-articles []
  (go
    (let [response (json-parse
                     (:body (<! (http/get "/top_articles"))))
          data (get response "data")]
      (render-element-with-param "articles" articles data)
      (render-element-with-param "images" images data))))

(defn login [username password]
  (go
    (let [response (json-parse
                     (:body (<! (http/get (str "/login?username=" username "&password=" password)))))
          nick (get response "nick")]
      (println "Hello" nick)
      (get-top-articles)
      (render-element-with-param "greeting" greeting nick)
      (swap! app-state assoc :logged-in? true))))

(defn home []
  (let [email-address (atom nil)
        password (atom nil)]
    (fn []
      [:div {:class "signup-wrapper"}
       [:h2 "Kansanmuisti"]
       (when-not (:logged-in? @app-state)
         [:form
          (wrap-as-element-in-form [email-form email-address])
          (wrap-as-element-in-form [password-form password])
          [:button {:type "submit"
                    :class "btn btn-default"
                    :on-click #(do (login @email-address @password)
                                   false)}
           "Kirjaudu YleTunnuksella"]])])))

(reagent/render-component [home]
                          (. js/document (getElementById "app")))


(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)

