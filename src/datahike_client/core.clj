(ns datahike-client.core
    (:require [clojure.walk :as walk]
              [ajax.core :as http]
              [taoensso.timbre :as log]))

(defn ->op
  "{req-method1 {summary     summary
                 operationId id
                 ...}} -> {method1 :req-method1
                           op      :op
                           doc     doc
                           params  [params]}"
  [desc]
  (let [method (key desc)
        data   (val desc)
        op     (data "operationId")
        doc    (format "%s\n%s"
                       (data "summary")
                       (data "description"))
        params (map #(dissoc % "description") (data "parameters"))]
    {:method (keyword method)
     :op     (keyword op)
     :doc    doc
     :params (walk/keywordize-keys params)}))

(defn ->endpoint
  "{path {req-method1 {meta-data..}}
          req-method2 {meta-data..}
          ...} -> {:path path :ops [output-of->op]}"
  [[path op-defs]]
  {:path path
   :ops  (map ->op op-defs)})

(defn handler [response]
  response)

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

(deftype Client [uri spec])

(defn client [{:keys [uri]}]
  (let [a (atom {})
        h (fn [v] (reset! a v))
        o (http/GET (str uri "/swagger.json") {:handler h})]
    (->Client uri a)))

(defn ops
  "Returns the supported ops for a client."
  [client]
  (->> @(.spec client)
       :paths
       (map ->endpoint)
       #_(mapcat :ops)
       #_(map :op)))

(comment
  (def foo (http/GET "http://localhost:3000/swagger.json" {:handler handler}))
  (await (api-request :get "http://localhost:3000/swagger.json"))
  (api-request :post "http://localhost:3000/transact" "{:tx-data {:bar :baz}}")
  (def c (client {:uri "http://localhost:3000"}))
  (ops c)
  (map ->endpoint (:paths @(.spec c)))
  (def datoms (get-in @(.spec c) [:paths "/datoms"]))
  (get-in @(.spec c) [:paths "/seek-datoms"])
  (->endpoint ["/datoms" datoms]))
