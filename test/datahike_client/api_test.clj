(ns datahike-client.api-test
  (:require [clojure.test :refer :all]
            [datahike-client.api :as sut]))

(defonce config {:timeout 300
                 :endpoint "http://localhost:3000"
                 :db-name "config-test"})

(def client (sut/client config))

(def connection (sut/connect client (:db-name config)))

(defn init-db [f]
  (Thread/sleep 3000)
  (sut/transact connection {:db-name (:db-name config)
                            :tx-data [{:name  "Alice", :age   20}
                                      {:name  "Bob", :age   30}
                                      {:name  "Charlie", :age   40}
                                      {:age 15}]})
  (f))

(use-fixtures :once init-db)

#_(deftest client-test
    (testing "Successful client creation"
      (is (instance? Client client))))

#_(deftest connection-test
    (testing "Successful connection creation"
      (is (instance? Connection connection))))

(deftest list-databases-test
  (testing "Successful list-databases"
    (is (= [true {:databases [{:store {:id "default", :backend :mem}
                               :keep-history? true
                               :schema-flexibility :read
                               :name (:db-name config)
                               :index :datahike.index/hitchhiker-tree}]}]
           (sut/list-databases client)))))

(deftest pull-test
  (testing "Successful pull"
    (is (= [true {:age 15}]
           (sut/pull connection {:selector [:age :name] :eid 4})))
    (is (= [true {:age 15}]
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
    (is (= [true [{:age 40, :name "Charlie"} {:age 15}]]
           (sut/pull-many connection {:selector [:age :name] :eids [3 4]})))
    (is (= [true [{:age 40, :name "Charlie"} {:age 15}]]
           (sut/pull-many connection {:selector [:age :name] :eids [3 4]})))))

(deftest q-test
  (testing "Successful q"
    (is (= [true [["Charlie"]]]
           (sut/q connection {:query '[:find ?v
                                       :where [_ :name ?v]]
                              :args []
                              :limit 1
                              :offset 0})))
    (is (= [true [["Charlie"]]]
           (sut/q connection
                  '[:find ?v
                    :where [_ :name ?v]]
                  []
                  1
                  0)))
    (is (= [true [["Charlie"] ["Alice"] ["Bob"]]]
           (sut/q connection
                  {:query '[:find ?v
                            :where [_ :name ?v]]})))))

(deftest datoms-test
  (testing "Successful datoms index only"
    (is (= 8
           (-> (sut/datoms connection {:index :eavt})
               second
               count)))
    (is (= 8
           (-> (sut/datoms connection :eavt)
               second
               count))))
  (testing "Successful datoms index and components"
    (is (= [true [[4 :age 15 536870913 true]]]
           (sut/datoms connection {:index :eavt :components [4]})))
    (is (= [true [[4 :age 15 536870913 true]]]
           (sut/datoms connection :eavt 4)))
    (is (= [true [[4 :age 15 536870913 true]]]
           (sut/datoms connection :eavt 4 :age)))
    (is (= [true []]
           (sut/datoms connection :eavt 4 :age 16)))))

(deftest seek-datoms-test
  (testing "Successful datoms index only"
    (is (= 8
           (-> (sut/seek-datoms connection {:index :eavt})
               second
               count)))
    (is (= 8
           (-> (sut/seek-datoms connection :eavt)
               second
               count))))
  (testing "Successful datoms index and components"
    (is (= [4 :age 15 536870913 true]
           (-> (sut/seek-datoms connection {:index :eavt :components [4]})
               second
               first)))
    (is (= [4 :age 15 536870913 true]
           (-> (sut/seek-datoms connection :eavt 4)
               second
               first)))
    (is (= [4 :age 15 536870913 true]
           (-> (sut/seek-datoms connection :eavt 4 :age)
               second
               first)))
    (is (= :db/txInstant
           (-> (sut/seek-datoms connection :eavt 4 :age 16)
               second
               first
               second)))))

(deftest db-test
  (testing "Successful db"
    (is (let [res (sut/db connection)]
          (and (true? (first res))
               (contains? (second res) :tx))))))
