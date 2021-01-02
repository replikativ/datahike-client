(ns datahike-client.request
  (:require [ajax.core :as http])
  (:import [datahike-client.core Client]))

(defn invoke [client {:keys [uri method params headers timeout]}]
  {:pre [(instance? Client client)]}
  (let [response                         (atom {})
        handler                          (fn [res] (reset! response res))
        request-map                       {:uri             (str (.endpoint client) uri)
                                           :method          method
                                           :format          (http/transit-request-format)
                                           :response-format (http/transit-response-format)
                                           :headers         headers
                                           :timeout         (or timeout 300)
                                           :params          params
                                           :handler         handler}]
    @(http/ajax-request request-map)
    @response
    #_(let [res @response]
        (if (first res)
          (second res)
          (throw (ex-info "Failed request" {:request request-map
                                            :cause (second res)}))))))
