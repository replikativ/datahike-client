(ns datahike-client.core
  (:require [clj-http.client :as client]
            [cognitect.transit :as transit])
  (:import [java.io ByteArrayInputStream ByteArrayOutputStream]))

(defn api-request [endpoint data]
  (let [out (ByteArrayOutputStream. 4096)
        writer (transit/writer out :msgpack)
        _ (transit/write writer data)
        resp (client/request {:content-type :transit+json
                              :accept :transit+json
                              :as :stream
                              :method :post
                              :url (str "http://localhost:3000/" endpoint)
                              :body (.toByteArray out)})
        parsed (client/parse-transit (:body resp) :msgpack)]
    (.close out)
    parsed))

(api-request "datoms" {:index :eavt})

(api-request "transact" {:tx-data [{:foo "BAR3" :bar 8}]})

(api-request "transact" {:tx-data [{:db/id -1 :foo "BAR4" :bar 16}]})

(api-request "q" {:query '[:find ?e ?b :where [?e :foo ?b]]})
