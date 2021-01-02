(ns datahike-client.request
  (:require [ajax.core :as http]
            [taoensso.timbre :as log])
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
                                           :handler         handler}
        _                                (log/debug request-map)]
    @(http/ajax-request request-map)
    @response
    #_(let [res @response
            _ (log/debug res)]
        (if (first res)
          (second res)
          (throw (ex-info "Failed request" {:request request-map
                                            :cause (second res)}))))))
