(ns datahike-client.core
    (:require [clojure.walk :as walk]
              [ajax.core :as http]
              [taoensso.timbre :as log]))

(defonce config {:timeout 300
                 :endpoint "http://localhost:3000"
                 :token "bar"
                 :db-name "foo"})

(deftype Client [endpoint token])

(defn client [{:keys [endpoint token]}]
  (->Client endpoint token))

(deftype Connection [client db-name])

(defn connect [client db-name]
  {:pre [(instance? Client client)
         (string? db-name)]}
  (->Connection client db-name))

(defn invoke [client {:keys [uri method params headers]}]
  {:pre [(instance? Client client)]}
  (let [response                         (atom {})
        handler                          (fn [res] (reset! response res))
        request-map                       {:uri             (str (.endpoint client) uri)
                                           :method          method
                                           :format          (http/transit-request-format)
                                           :response-format (http/transit-response-format)
                                           :headers         headers
                                           :timeout         (:timeout config)
                                           :params          params
                                           :handler         handler}
        _                                (clojure.pprint/pprint request-map)]
    @(http/ajax-request request-map)
    @response))

(defn list-databases
  ([client]
   (invoke client nil))
  ([client arg-map]
   {:pre [(instance? Client client)
          (map? arg-map)]}
   (invoke client {:uri "/databases"
                   :method :get})))

(defn transact [conn arg-map]
  {:pre [(instance? Connection conn)
         (map? arg-map)]}
  (invoke (.client conn)
          {:uri "/transact"
           :params {:tx-data (:tx-data arg-map)}
           :method :post
           :headers {"db-name" (.db-name conn)}}))

(defn pull
  ([conn arg-map]
   {:pre [(instance? Connection conn)
          (map? arg-map)]}
   (let [db-tx (:db-tx arg-map)]
     (invoke (.client conn)
             {:uri "/pull"
              :params (dissoc arg-map :db-tx)
              :method :post
              :headers (merge {"db-name" (.db-name conn)}
                              (when db-tx
                                {"db-tx" db-tx}))})))
  ([conn selector eid]
   (pull conn {:selector selector :eid eid}))
  ([conn selector eid db-tx]
   (pull conn db-tx {:selector selector :eid eid :db-tx db-tx})))

(defn db [conn]
  {:pre [(instance? Connection conn)]}
  (invoke (.client conn)
          {:uri "/db"
           :method :get
           :headers {"db-name" (.db-name conn)}}))

(comment
  (def c (client config))
  (.endpoint c)
  (instance? Client c)

  (list-databases c {})
  (def conn (connect c "foo"))
  (transact conn {:db-name (:db-name config)
                  :tx-data [{:name  "Alice", :age   20}
                            {:name  "Bob", :age   30}
                            {:name  "Charlie", :age   40}
                            {:age 15}]})
  (pull conn {:selector [:age :name] :eid 4})
  (pull conn {:selector [:age :name] :eid 4 :db-tx "12345556"})
  ; TODO
  (pull conn {:selector [:age :name] :eid 4 :db-tx 12345556})
  (merge {"db-name" "foo"})
  (db conn))
