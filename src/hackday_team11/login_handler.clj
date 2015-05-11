(ns hackday-team11.login-handler
  (:require [org.httpkit.client :as http]
            [compojure.core :refer :all]
            [ring.util.codec :refer [form-decode]]
            [clojure.walk :refer [keywordize-keys]]
            [compojure.route :as route]))

(def login-api-app-id (System/getenv "LOGIN_API_APP_ID"))

(def login-api-app-key (System/getenv "LOGIN_API_APP_KEY"))

(defroutes app
  (GET "/login" [username password :as request]
       (let [params (keywordize-keys (form-decode (:query-string request)))]
         (:body @(http/post (str "https://login.api.yle.fi/v1/user/login?app_id=" login-api-app-id "&app_key=" login-api-app-key)
                            {:form-params {"username" (:username params) "password" (:password params)}})))))
