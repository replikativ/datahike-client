(ns datahike-client.core
    (:require [clojure.walk :as walk]
              [ajax.core :as http]
              [taoensso.timbre :as log]))

(defn ->op
  "{:req-method1 {:summary     summary
                  :operationId id
                 ...}} -> {:method1 :req-method1
                           :op      :op
                           :doc     doc
                           :params  [params]}"
  [desc path]
  (let [method       (key desc)
        data         (val desc)
        operation-id (keyword (data :operationId))
        doc          (format "%s\n%s"
                             (data :summary)
                             (data :description))
        params       (map #(dissoc % :description) (data :parameters))]
    {operation-id
     {:path path
      :method method
      :doc    doc
      :params (walk/keywordize-keys params)}}))

(defn ->endpoint
    "{:path {:req-method1 {meta-data..}}
           :req-method2 {meta-data..}
          ...} -> {:path path :ops [output-of->op]}"
    [[path op-defs]]
    (map #(->op % path) op-defs))

(defn find-op-meta
  "Finds the matching operation by operationId in list of ops from the spec.
  Returns the method, doc and params form it, nil otherwise."
  [op ops]
  (->> ops
       (filter #(= op (:operation-id %)))
       (map #(select-keys % [:method :doc :params]))
       first))

(defn gather-request-params
  "Reducer fn generating categorized params from supplied params.
  Returns a map of query, path and body params."
  [supplied-params request-params {:keys [name in]}]
  (let [param (keyword name)]
    (if (not (contains? supplied-params param))
      request-params
      (update-in request-params [(keyword in)] assoc param (param supplied-params)))))

(defn invoke
  ([operation client]
   (invoke operation client nil))
  ([operation client params]
   {:pre [(instance? Client client)
          (and (keyword? operation) (contains? (.operations client) operation))]}
   (let [request-info                     (operation (.operations client))
         {:keys [body query header path]} (->> request-info
                                              :params
                                              (reduce (partial gather-request-params params) {}))
         response                         (atom {})
         handler                          (fn [res] (reset! response res))
         request-map                       {:uri             (str (.endpoint client) (:path request-info))
                                            :method          (:method request-info)
                                            :format          (http/json-request-format)
                                            :response-format (http/json-response-format)
                                            :headers         header
                                            :timeout         300
                                            :params          body
                                            :handler         handler}
         _                                (clojure.pprint/pprint request-map)]
     @(http/ajax-request request-map)
     @response)))

(deftype Client [endpoint secret operations])

(defn client [{:keys [endpoint secret]}]
  (let [spec (atom {})
        handler (fn [v] (reset! spec v))
        o @(http/GET (str endpoint "/swagger.json") {:handler handler})
        operations (->> @spec
                        :paths
                        (map ->endpoint)
                        (flatten)
                        (into {}))]
    (->Client endpoint secret operations)))

(deftype Connection [client db-name])

(defn connect [client db-name]
  {:pre [(instance? Client client)
         (string? db-name)]}
  (->Connection client db-name))

(defn list-databases [client arg-map]
  {:pre [(instance? Client client)
         (map? arg-map)]}
  (invoke :ListDatabases client))

(defn transact [conn arg-map]
  {:pre [(instance? Connection conn)
         (map? arg-map)]}
  (invoke :Transact (.client conn) arg-map))

(comment
  (def c (client {:endpoint "http://localhost:3000" :secret "bar"}))
  (.endpoint c)
  (keys (.operations c))
  (instance? Client c)

  (list-databases c {})
  (def conn (connect c "abrupt-red-deer"))
  (transact conn {:body {:transactions {:tx-data [{:name  "Alice", :age   20}
                                                  {:name  "Bob", :age   30}
                                                  {:name  "Charlie", :age   40}
                                                  {:age 15}]}}})

  (def paths (->> @(.spec c)
                 :paths
                 (map ->endpoint)
                 (clojure.walk/keywordize-keys)
                 (flatten)
                 (into {})))
  (contains? (.endpoints c) :SeekDatoms)
  (:EchoPOST (.endpoints c))
  (invoke :EchoGET c)
  (invoke :EchoPOST c {:datahike-server.server/params {:foo "bar"}})
  (->> (:EchoPOST (.endpoints c))
       :params
       (reduce (partial gather-request-params
                        {"datahike-server.server/params" {:db-name "faulty-least-weasel"}})
               {}))
  (->> (:SeekDatoms (.endpoints c))
       :params
       (reduce (partial gather-request-params
                        {:datahike-server.server/datoms-request {:index :aevt}
                         :db-name "fortunate-common-pipistrelle"})
               {}))
  (invoke :EchoGET c)
  (invoke :SeekDatoms c {:datahike-server.server/datoms-request {:index :aevt}
                         :db-name "fortunate-common-pipistrelle"}))
