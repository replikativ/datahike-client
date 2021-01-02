# datahike-client

Datahike Client is a library to interact with Datahike Server from your Clojure and
Clojurescript applications.

This project is early stage and considered instable. Expect changes to the API and
to the underlying parts.

## Usage

```
(require [datahike-client.api :as d]))

(defonce config {:timeout 300
                 :endpoint "http://localhost:3000"
                 :token "secret"
                 :db-name "config-test"})

(def client (d/client config))

(def connection (d/connect client (:db-name config)))

(d/transact connection {:db-name (:db-name config)
                        :tx-data [{:name  "Alice", :age   20}
                                  {:name  "Bob", :age   30}
                                  {:name  "Charlie", :age   40}
                                  {:age 15}]})

(d/q connection
     {:query '[:find ?v
      :where [_ :name ?v]]}) ; =>  [["Charlie"] ["Alice"] ["Bob"]]
```

Take a look at [cljdoc.org](https://cljdoc.org/d/io.replikativ/datahike-client).
