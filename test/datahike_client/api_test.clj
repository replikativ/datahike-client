(ns datahike-client.api-test
  (:require [clojure.test :refer :all]
            [datahike-client.api :as sut]))

(defonce config {:timeout 300
                 :endpoint "http://localhost:3000"
                 :token "bar"
                 :db-name "config-test"})

(def client (sut/client config))

(def connection (sut/connect client (:db-name config)))

#_(deftest client-test
    (testing "Successful client creation"
      (is (instance? Client client))))

#_(deftest connection-test
    (testing "Successful connection creation"
      (is (instance? Connection connection))))

(deftest list-databases-test
  (testing "Successful list-databases"
    (is (= {:databases [{:store {:id "default", :backend :mem}
                         :keep-history? true
                         :schema-flexibility :read
                         :name (:db-name config)
                         :index :datahike.index/hitchhiker-tree}]}
           (sut/list-databases client)))))

(deftest transact-test
  (testing "Successful transact"
    (is (= {:tx-data [[536870923 :db/txInstant #inst "2020-12-29T17:42:46.963-00:00" 536870923 true]
                      [41 :name "Alice" 536870923 true]
                      [41 :age 20 536870923 true]
                      [42 :name "Bob" 536870923 true]
                      [42 :age 30 536870923 true]
                      [43 :name "Charlie" 536870923 true]
                      [43 :age 40 536870923 true]
                      [44 :age 15 536870923 true]]
            :tempids #:db{:current-tx 536870923}, :tx-meta []}
           (sut/transact connection {:db-name (:db-name config)
                                     :tx-data [{:name  "Alice", :age   20}
                                               {:name  "Bob", :age   30}
                                               {:name  "Charlie", :age   40}
                                               {:age 15}]})))))

(deftest pull-test
  (testing "Successful pull"
    (is (= {:age 15}
           (sut/pull connection {:selector [:age :name] :eid 4})))
    (is (= {:age 15}
           (sut/pull connection [:age :name] 4))))
  ; TODO throws classcast exception
  #_(testing "Successful pull with db-tx"
      (is (thrown? java.lang.ClassCastException (sut/pull connection {:selector [:age :name] :eid 4 :db-tx 12345556})))
      (is (thrown? java.lang.ClassCastException (sut/pull connection [:age :name] 4 12345556))))
  #_(testing "Failed with db-tx as String"
      (is (= "valAt/2 is not supported on AsOfDB"
             (-> (sut/pull connection {:selector [:age :name] :eid 4 :db-tx "12345556"})
                 (get :status-text))))))

(deftest pull-many-test
  (testing "Successful pull-many"
    (is (= [{:age 40, :name "Charlie"} {:age 15}]
           (sut/pull-many connection {:selector [:age :name] :eids [3 4]})))
    (is (= [{:age 40, :name "Charlie"} {:age 15}]
           (sut/pull-many connection {:selector [:age :name] :eids [3 4]})))))

(deftest db-test
  (testing "Successful db"
    (is (contains? (sut/db connection) :tx))))
