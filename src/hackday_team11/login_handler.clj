(ns hackday-team11.login-handler
  (:require [org.httpkit.client :as http]
            [compojure.core :refer :all]
            [cheshire.core :refer [parse-string]]
            [ring.util.codec :refer [form-decode]]
            [clojure.walk :refer [keywordize-keys]]
            [compojure.route :as route])
  (:import  [java.net URLDecoder]))

(def login-api-app-id (System/getenv "LOGIN_API_APP_ID"))

(def login-api-app-key (System/getenv "LOGIN_API_APP_KEY"))

(def app-id (System/getenv "APP_ID"))

(def app-key (System/getenv "APP_KEY"))

(defn get-hash-tags [text]
  (->> (clojure.string/split text #" ")
       (filter #(.startsWith % "#"))
       (map #(.substring % 1 ))))

(defn- articles-auth-params []
  (str "app_id=" (System/getenv "ARTICLES_APP_ID") "&app_key=" (System/getenv "ARTICLES_APP_KEY")))

(defn get-proposed-tags [text]
  (let [hash-tags (get-hash-tags text)
        data (:body @(http/get (str "http://meta.api.yle.fi/v1/concept_search.json?q="
                                    (first hash-tags)
                                    "&app_id=" app-id "&app_key=" app-key)))]
    data))

(defn get-yle-id [exact-match]
  (let [body (:body @(http/get (str "http://meta.api.yle.fi/v1/concepts.json?exactmatch="
                                    exact-match
                                    "&app_id=" app-id "&app_key=" app-key)))]
  (-> (parse-string body true) :data first :id)))


(defn get-top-articles [exact-match]
    (let [yle-id (get-yle-id exact-match)
          meta-data (parse-string (:body @(http/get (str "http://meta.api.yle.fi/v1/content.json?language=fi"
                                                         "&subject=" yle-id
                                                         "&app_id=" app-id "&app_key=" app-key))) true)
          article-ids (clojure.string/join "," (take 10 (map :id (:data meta-data))))]
          (println exact-match)
          (println yle-id)
          (println article-ids)
          (:body @(http/get (str "http://articles.api.yle.fi/v1/items.json?id="
                                 article-ids
                                 "&fields=title,lead,url,image&"
                                 (articles-auth-params))))))

(defroutes app
  (POST "/proposed_tags" {body :body}
      (let [text (->> (URLDecoder/decode (slurp body) "UTF-8")
                      (form-decode)
                      (keywordize-keys)
                      :text)]
         (println text)
         (get-proposed-tags text)))
  (POST "/top_articles" {body :body}
      (let [exact-match (->> (URLDecoder/decode (slurp body) "UTF-8")
                      (form-decode)
                      (keywordize-keys)
                      :exact-match)]
         (println exact-match)
         (get-top-articles exact-match)))
  (GET "/login" [username password :as request]
       (let [params (keywordize-keys (form-decode (:query-string request)))]
         (:body @(http/post (str "https://login.api.yle.fi/v1/user/login?app_id=" login-api-app-id "&app_key=" login-api-app-key)
                            {:form-params {"username" (:username params) "password" (:password params)}})))))
