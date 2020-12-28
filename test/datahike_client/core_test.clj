(ns datahike-client.core-test
  (:require [clojure.test :refer :all]
            [datahike-client.core :as sut]))

(defonce config {:timeout 300
                 :endpoint "http://localhost:3000"
                 :token "bar"
                 :db-name "foo"})

(def client (sut/client config))

(def connection (sut/connect client "foo"))

(deftest list-databases-test
  (testing "Successful list-databases"
    (is (= "foo"
           (sut/list-databases client {})))))

(deftest transact-test
  (testing "Successful transact"
    (is (= "foo"
           (sut/transact connection {:db-name (:db-name config)
                                     :tx-data [{:name  "Alice", :age   20}
                                               {:name  "Bob", :age   30}
                                               {:name  "Charlie", :age   40}
                                               {:age 15}]})))))

(deftest pull-test
  (testing "Successful pull"
    (is (= "foo"
           (sut/pull connection {:selector [:age :name] :eid 4}))))
  (testing "Successful pull with db-tx"
    (is (= "foo"
           (sut/pull connection {:selector [:age :name] :eid 4 :db-tx "12345556"}))))
  (testing "Failed with db-tx as Long"
    (is (= "foo"
           (sut/pull connection {:selector [:age :name] :eid 4 :db-tx 12345556})))))
