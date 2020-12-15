(ns datahike-client.core
    (:require [clojure.walk :as walk]
              [ajax.core :as http]
              [taoensso.timbre :as log]))

(defn invoke [client {:keys [uri method params header]}]
  {:pre [(instance? Client client)]}
  (let [response                         (atom {})
        handler                          (fn [res] (reset! response res))
        request-map                       {:uri             (str (.endpoint client) uri)
                                           :method          method
                                           :format          (http/transit-request-format)
                                           :response-format (http/transit-response-format)
                                           :headers         header
                                           :timeout         300
                                           :params          params
                                           :handler         handler}
        _                                (clojure.pprint/pprint request-map)]
    @(http/ajax-request request-map)
    @response))

(deftype Client [endpoint secret])

(defn client [{:keys [endpoint secret]}]
  (->Client endpoint secret))

(deftype Connection [client db-name])

(defn connect [client db-name]
  {:pre [(instance? Client client)
         (string? db-name)]}
  (->Connection client db-name))

(defn list-databases
  ([client]
   (invoke client nil))
  ([client arg-map]
   {:pre [(instance? Client client)
          (map? arg-map)]}
   (invoke client {:uri "/databases"
                   :method :get})))

(defn transact [conn tx-map]
  {:pre [(instance? Connection conn)
         (map? tx-map)]}
  (invoke (.client conn) {:uri "/transact"
                          :params tx-map
                          :method :post}))

(comment
  (def c (client {:endpoint "http://localhost:3333" :secret "bar"}))
  (.endpoint c)
  (instance? Client c)

  (list-databases c {})
  (def conn (connect c "gorgeous-house-mouse"))
  (transact conn {:transactions {:tx-data [{:name  "Alice", :age   20}
                                           {:name  "Bob", :age   30}
                                           {:name  "Charlie", :age   40}
                                           {:age 15}]}}))
