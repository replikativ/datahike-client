(ns datahike-client.core
    (:require [java-http-clj.core :as http]))


(defn api-request
  ([method uri]
   (api-request method uri nil nil))
  ([method uri data]
   (api-request method uri data nil))
  ([method uri data opts]
   (http/send (merge {:uri uri
                      :method method
                      :headers {"Content-Type" "transit+json"}
                               "Accept" "transit+json"}
                     (when (or (= method :post) data)
                       {:body (str data)})))))

(comment
  (api-request :get "http://localhost:3333/swagger.json")
  (api-request :post "http://localhost:3333/swagger.json" "{:tx-data {:bar :baz}}"))
