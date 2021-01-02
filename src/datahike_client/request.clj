(ns datahike-client.request
  (:require [java-http-clj.core :as http]
            [cognitect.transit :as transit])
  (:import [java.io ByteArrayInputStream ByteArrayOutputStream]))

(defn invoke [client {:keys [uri method params headers timeout]}]
  (let [out (ByteArrayOutputStream. 4096)
        writer (transit/writer out :json)
        _ (transit/write writer params)
        request-map {:uri (str (.endpoint client) uri)
                     :method method
                     :headers (merge headers
                                     {"Accept" "application/transit+json"
                                      "Content-type" "application/transit+json"
                                      "Charset" "utf-8"}
                                     (when-let [token (.token client)]
                                       {"authorization" (str "token " token)}))
                     :timeout (or timeout 600)
                     :body (.toString out)}
        response     (http/send request-map {:as :input-stream})]
    (if (and (contains? response :status)
             (< (:status response) 400))
      (transit/read (transit/reader (:body response) :json))
      (throw (ex-info "Failed request" {:request request-map
                                        :response response})))))
