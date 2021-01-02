(ns datahike-client.core
  (:require [clojure.walk :as walk]
            [datahike-client.request :as r]))

(deftype Client [endpoint token])

(defn client [{:keys [endpoint token]}]
  (->Client endpoint token))

(deftype Connection [client db-name])

(defn connect [client db-name]
  {:pre [(instance? Client client)
         (string? db-name)]}
  (->Connection client db-name))

(defn list-databases
  ([client]
   (list-databases client {}))
  ([client arg-map]
   {:pre [(instance? Client client)
          (map? arg-map)]}
   (r/invoke client {:uri "/databases"
                     :method :get})))

(defn transact [conn arg-map]
  {:pre [(instance? Connection conn)
         (map? arg-map)]}
  (r/invoke (.client conn)
            {:uri "/transact"
             :params {:tx-data (:tx-data arg-map)}
             :method :post
             :headers {"db-name" (.db-name conn)}}))

(defn pull
  ([conn {:keys [selector eid db-tx]}]
   (pull conn selector eid db-tx))
  ([conn selector eid]
   (pull conn selector eid nil))
  ([conn selector eid db-tx]
   {:pre [(instance? Connection conn)
          (vector? selector)
          (int? eid)
          (or (int? db-tx)
              (nil? db-tx))]}
   (r/invoke (.client conn)
             {:uri "/pull"
              :params {:selector selector
                       :eid eid}
              :method :post
              :headers (merge {"db-name" (.db-name conn)}
                              (when db-tx
                                {"db-tx" db-tx}))})))

(defn pull-many
  ([conn {:keys [selector eids db-tx]}]
   (pull-many conn selector eids db-tx))
  ([conn selector eids]
   (pull-many conn selector eids))
  ([conn selector eids db-tx]
   {:pre [(instance? Connection conn)
          (vector? selector)
          (vector? eids)
          (or (int? db-tx)
              (nil? db-tx))]}
   (r/invoke (.client conn)
             {:uri "/pull-many"
              :params {:selector selector
                       :eids eids}
              :method :post
              :headers (merge {"db-name" (.db-name conn)}
                              (when db-tx
                                {"db-tx" db-tx}))})))

(defn q
  ([conn {:keys [query args limit offset db-tx]}]
   (r/invoke (.client conn)
             {:uri "/q"
              :method :post
              :params (merge {:query query}
                             (when args
                               {:args args})
                             (when limit
                               {:limit limit})
                             (when offset
                               {:offset offset}))
              :headers (merge {"db-name" (.db-name conn)}
                              (when db-tx
                                {"db-tx" db-tx}))}))
  ([conn query & args]
   (let [[args limit offset db-tx] args]
     (q conn
        {:query query
         :args args
         :limit limit
         :offset offset
         :db-tx db-tx}))))

(defn datoms
  ([conn index components]
   (datoms conn index components nil))
  ([conn index components db-tx]
   (r/invoke (.client conn)
             {:uri "/datoms"
              :method :post
              :params (merge {:index index}
                             (when components
                               {:components components}))
              :headers (merge {"db-name" (.db-name conn)}
                              (when db-tx
                                {"db-tx" db-tx}))})))

(defn seek-datoms
  ([conn index components]
   (seek-datoms conn index components nil))
  ([conn index components db-tx]
   (r/invoke (.client conn)
             {:uri "/seek-datoms"
              :method :post
              :params (merge {:index index}
                             (when components
                               {:components components}))
              :headers (merge {"db-name" (.db-name conn)}
                              (when db-tx
                                {"db-tx" db-tx}))})))

(defn entity
  ([conn eid]
   (entity conn eid nil))
  ([conn eid db-tx]
   (r/invoke (.client conn)
             {:uri "/entity"
              :method :post
              :params {:eid eid}
              :headers (merge {"db-name" (.db-name conn)}
                              (when db-tx
                                {"db-tx" db-tx}))})))

(defn db [conn]
  {:pre [(instance? Connection conn)]}
  (r/invoke (.client conn)
            {:uri "/db"
             :method :get
             :headers {"db-name" (.db-name conn)}}))

(comment
  (def config {:timeout 300
               :endpoint "http://localhost:3000"
               :token "secret"
               :db-name "config-test"})

  (def c (client config))
  (.token c)
  (instance? Client c)
  (list-databases c {})
  (def conn (connect c (:db-name config)))
  (.db-name conn)
  (transact conn {:tx-data [{:name  "Alice", :age   20}
                            {:name  "Bob", :age   30}
                            {:name  "Charlie", :age   40}
                            {:age 15}]})
  (pull conn {:selector [:age :name] :eid 4})
  (pull conn {:selector [:age :name] :eid 4 :db-tx "12345556"})
  ; TODO
  (pull conn {:selector [:age :name] :eid 4 :db-tx 12345556})
  (pull-many conn {:selector [:age :name] :eids [1 2 3 4]})
  (datahike-client.api/q
   conn
   {:query '[:find ?v
             :where [_ :name ?v]]})
  (datahike-client.api/datoms conn {:index :eavt})
  (datahike-client.api/datoms conn :eavt)
  (datahike-client.api/seek-datoms conn :eavt)
  (datahike-client.api/entity conn 3))
