(defproject io.replikativ/datahike-client "0.1.0-SNAPSHOT"
  :description "Datahike client to connect to Datahike server."
  :license {:name "Eclipse"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :url "https://github.com/replikativ/datahike-client"

  :dependencies [[org.clojure/clojure "1.10.1" :scope "provided"]
                 [org.clojure/clojurescript "1.9.854" :scope "provided"]
                 [cljs-ajax "0.8.1"]
                 [com.cognitect/transit-clj "1.0.324"]
                 [com.cognitect/transit-cljs "0.8.264"]
                 [com.taoensso/timbre "5.1.0"]])
