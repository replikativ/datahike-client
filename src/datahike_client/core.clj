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

(deftype Client [uri spec endpoints ops])

(defn client [{:keys [uri]}]
  (let [spec (atom {})
        handler (fn [v] (reset! spec v))
        o @(http/GET (str uri "/swagger.json") {:handler handler})
        endpoints (->> @spec
                       :paths
                       (map ->endpoint)
                       (flatten)
                       (into {}))
        ops (keys endpoints)]
    (->Client uri spec endpoints ops)))

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
          (and (keyword? operation) (contains? (.endpoints client) operation))]}
   (let [request-info                     (operation (.endpoints client))
         {:keys [body query header path]} (->> request-info
                                              :params
                                              (reduce (partial gather-request-params params) {}))
         response                         (atom {})
         handler                          (fn [res] (reset! response res))
         request-map                       {:uri             (str (.uri client) (:path request-info))
                                            :method          (:method request-info)
                                            :format          (http/transit-request-format)
                                            :response-format (http/transit-response-format)
                                            :headers         header
                                            :timeout         30
                                            :params          (or body query)
                                            :handler         handler}
         _                                (clojure.pprint/pprint request-map)]
     @(http/ajax-request {:uri             (str (.uri client) (:path params))
                          :method          (:method request-info)
                          :format          (http/transit-request-format)
                          :response-format (http/transit-response-format)
                          :headers         header
                          :timeout         30
                          :params          (or body query)
                          :handler         handler})
     @response)))


(comment
  (def c (client {:uri "http://localhost:3000"}))
  (.endpoints c)
  (.ops c)

  (instance? Client c)
  (def paths (->> @(.spec c)
                 :paths
                 (map ->endpoint)
                 (clojure.walk/keywordize-keys)
                 (flatten)
                 (into {})))
  (contains? (.endpoints c) :SeekDatoms)
  (:SeekDatoms (.endpoints c))
  (invoke :EchoGET c {:path "/db"})
  (->> (:SeekDatoms (.endpoints c))
       :params
       (reduce (partial gather-request-params
                        {:datahike-server.server/datoms-request {:index :aevt}
                         :db-name "fortunate-common-pipistrelle"})
               {}))
  (invoke :EchoGET c)
  (invoke :SeekDatoms c {:datahike-server.server/datoms-request {:index :aevt}
                         :db-name "fortunate-common-pipistrelle"}))
