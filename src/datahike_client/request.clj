(ns datahike-client.request
  (:require [org.httpkit.client :as http]
            [org.httpkit.sni-client :as sni-client]
            [cognitect.transit :as transit])
  (:import [java.io ByteArrayInputStream ByteArrayOutputStream]))

(defn invoke [client {:keys [uri method params headers timeout]}]
  (binding [org.httpkit.client/*default-client* sni-client/default-client]
    (let [out (ByteArrayOutputStream. 4096)
          writer (transit/writer out :json)
          _ (transit/write writer params)
          request-map {:url (str (.endpoint client) uri)
                       :method method
                       :headers (merge headers
                                       {"Accept" "application/transit+json"
                                        "Content-type" "application/transit+json"
                                        "Charset" "utf-8"}
                                       (when-let [token (.token client)]
                                         {"authorization" (str "token " token)}))
                       :timeout (or timeout 600)
                       :body (.toString out)
                       :as :stream}
          response @(http/request request-map)]
      (if (and (contains? response :status)
               (< (:status response) 400))
        (transit/read (transit/reader (:body response) :json))
        (throw (ex-info "Failed request" {:request request-map
                                          :response response}))))))

