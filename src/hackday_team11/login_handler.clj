(ns hackday-team11.login-handler
  (:require [org.httpkit.client :as http]
            [compojure.core :refer :all]
            [cheshire.core :refer [parse-string]]
            [ring.util.codec :refer [form-decode]]
            [clojure.walk :refer [keywordize-keys]]
            [compojure.route :as route]))

(def login-api-app-id (System/getenv "LOGIN_API_APP_ID"))

(def login-api-app-key (System/getenv "LOGIN_API_APP_KEY"))

(def app-id (System/getenv "APP_ID"))

(def app-key (System/getenv "APP_KEY"))

(defn- articles-auth-params []
  (str "app_id=" (System/getenv "ARTICLES_APP_ID") "&app_key=" (System/getenv "ARTICLES_APP_KEY")))

(defn get-top-articles []
    (let [some-data (parse-string (:body @(http/get (str "http://somedata.api.yle.fi/v2/articles/most_read.json?period=6h&section.publication=aihe&language=fi&app_id=" app-id "&app_key=" app-key))) true)
          article-ids (clojure.string/join "," (take 10 (map :yleId (:data some-data))))]
          (:body @(http/get (str "http://articles.api.yle.fi/v1/items.json?id="
                                 article-ids
                                 "&fields=title,lead,url,image&"
                                 (articles-auth-params))))))

(defroutes app
  (GET "/top_articles" [] (get-top-articles))
  (GET "/login" [username password :as request]
       (let [params (keywordize-keys (form-decode (:query-string request)))]
         (:body @(http/post (str "https://login.api.yle.fi/v1/user/login?app_id=" login-api-app-id "&app_key=" login-api-app-key)
                            {:form-params {"username" (:username params) "password" (:password params)}})))))
