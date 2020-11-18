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

(defn api-request
  ([method uri]
   (api-request method uri nil nil))
  ([method uri data]
   (api-request method uri data nil))
  ([method uri data opts]
   (let [_ (log/debug "Sending request: " method " " uri " " data " " opts)]
     (http/ajax-request (merge {:uri uri
                                :method method
                                :format (http/transit-request-format)
                                :response-format (http/transit-response-format)
                                :handler (fn [r] r)
                                :params data})))))

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

(defn request-info-of
  "Returns a map of path, method, doc and params of an operation.
   Returns nil if not found."
  [operation client]
  (let [{:keys [paths version]} (get-path-of-operation operation client)]
    (->> paths
         (map ->endpoint)
         (map #(assoc-in % [:ops] (find-op-meta operation (:ops %))))
         (filter #(some? (:ops %)))
         (map #(hash-map :path   (format "/v%s%s"
                                         version
                                         (:path %))
                         :method (get-in % [:ops :method])
                         :doc    (get-in % [:ops :doc])
                         :params (get-in % [:ops :params])))
         first)))

#_(defn invoke
    ([operation client]
     (invoke operation client nil))
    ([operation client data]
     {:pre [(instance? Client client)
            (not nil? operation)]}
     (let [request-info                     (spec/request-info-of operation client)
           _                                (when (nil? request-info)
                                              (req/panic! "Invalid params for invoking op."))
           {:keys [body query header path]} (->> request-info
                                                 :params
                                                 (reduce (partial spec/gather-request-params params) {}))
           response                         (req/fetch {:conn             (req/connect* {:uri      (:uri conn)
                                                                                         :timeouts (:timeouts conn)})
                                                        :url              (:path request-info)
                                                        :method           (:method request-info)
                                                        :query            query
                                                        :header           header
                                                        :body             (-> body
                                                                              vals
                                                                              first)
                                                        :path             path
                                                        :as               as
                                                        :throw-exception? throw-exception?})
           try-json-parse                   #(try
                                               (json/read-value % (json/object-mapper {:decode-key-fn true}))
                                               (catch Exception _ %))]
       (case as
         (:socket :stream) response
         (try-json-parse response)))))


(comment
  (def c (client {:uri "http://localhost:3000"}))
  (.endpoints c)
  (.ops c)

  (def paths (->> @(.spec c)
                 :paths
                 (map ->endpoint)
                 (clojure.walk/keywordize-keys)
                 (flatten)
                 (into {}))))
