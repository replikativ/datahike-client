(ns datahike-client.api
  (:require [datahike-client.core :as c]))

(def
  ^{:arglists '([arg-map])
    :doc "Create a client for a Datahike Server.

          Usage:

          Create a client for a Datahike Server that runs locally:
           `(client {:endpoint \"https://localhost:3000\" :access-key \"foo\" :secret \"bar\")`"}
  client
  c/client)

(def
  ^{:arglists '([client db-name])
    :doc "Connects to a datahike database via client and name of db. For more information on the configuration refer to the [docs](https://github.com/replikativ/datahike-client).

          Usage:

          Connect to a Datahike server:
           `(connect client \"mydb\")`"}

  connect
  c/connect)

(def
  ^{:arglists '([client][client arg-map])
    :doc "Lists all databases.
          Usage:

              (list-databases client)"}
  list-databases
  c/list-databases)

(def ^{:arglists '([conn arg-map])
       :doc      "Applies transaction to the underlying database value and atomically updates the connection reference to point to the result of that transaction, the new db value.

                  Accepts the connection and a map or a vector as argument, specifying the transaction data.

                  Returns transaction report, a map:

                      {:db-before ...       ; db value before transaction
                       :db-after  ...       ; db value after transaction
                       :tx-data   [...]     ; plain datoms that were added/retracted from db-before
                       :tempids   {...}     ; map of tempid from tx-data => assigned entid in db-after
                       :tx-meta   tx-meta } ; the exact value you passed as `tx-meta`

                  Note! `conn` will be updated in-place and is not returned from [[transact]].

                  Usage:

                      ;; add a single datom to an existing entity (1)
                      (transact conn [[:db/add 1 :name \"Ivan\"]])

                      ;; retract a single datom
                      (transact conn [[:db/retract 1 :name \"Ivan\"]])

                      ;; retract single entity attribute
                      (transact conn [[:db.fn/retractAttribute 1 :name]])

                      ;; retract all entity attributes (effectively deletes entity)
                      (transact conn [[:db.fn/retractEntity 1]])

                      ;; create a new entity (`-1`, as any other negative value, is a tempid
                      ;; that will be replaced with DataScript to a next unused eid)
                      (transact conn [[:db/add -1 :name \"Ivan\"]])

                      ;; check assigned id (here `*1` is a result returned from previous `transact` call)
                      (def report *1)
                      (:tempids report) ; => {-1 296, :db/current-tx 536870913}

                      ;; check actual datoms inserted
                      (:tx-data report) ; => [#datahike/Datom [296 :name \"Ivan\" 536870913]]

                      ;; tempid can also be a string
                      (transact conn [[:db/add \"ivan\" :name \"Ivan\"]])
                      (:tempids *1) ; => {\"ivan\" 5, :db/current-tx 536870920}

                      ;; reference another entity (must exist)
                      (transact conn [[:db/add -1 :friend 296]])

                      ;; create an entity and set multiple attributes (in a single transaction
                      ;; equal tempids will be replaced with the same unused yet entid)
                      (transact conn [[:db/add -1 :name \"Ivan\"]
                                      [:db/add -1 :likes \"fries\"]
                                      [:db/add -1 :likes \"pizza\"]
                                      [:db/add -1 :friend 296]])

                      ;; create an entity and set multiple attributes (alternative map form)
                      (transact conn [{:db/id  -1
                                       :name   \"Ivan\"
                                       :likes  [\"fries\" \"pizza\"]
                                       :friend 296}])

                      ;; update an entity (alternative map form). Can’t retract attributes in
                      ;; map form. For cardinality many attrs, value (fish in this example)
                      ;; will be added to the list of existing values
                      (transact conn [{:db/id  296
                                       :name   \"Oleg\"
                                       :likes  [\"fish\"]}])

                      ;; ref attributes can be specified as nested map, that will create a nested entity as well
                      (transact conn [{:db/id  -1
                                       :name   \"Oleg\"
                                       :friend {:db/id -2
                                       :name \"Sergey\"}}])

                      ;; schema is needed for using a reverse attribute
                      (is (transact conn [{:db/valueType :db.type/ref
                                           :db/cardinality :db.cardinality/one
                                           :db/ident :friend}]))

                      ;; reverse attribute name can be used if you want a created entity to become
                      ;; a value in another entity reference
                      (transact conn [{:db/id  -1
                                       :name   \"Oleg\"
                                       :_friend 296}])
                      ;; equivalent to
                      (transact conn [[:db/add  -1 :name   \"Oleg\"]
                                      {:db/add 296 :friend -1]])"}
  transact
  c/transact)

(def ^{:arglists '([conn arg-map][conn selector eid][conn selector eid db-tx])
       :doc      "Fetches data from database using recursive declarative description. See [docs.datomic.com/on-prem/pull.html](https://docs.datomic.com/on-prem/pull.html).

                  Unlike [[entity]], returns plain Clojure map (not lazy).

                  Usage:

                      (pull conn \"my-db\" [:db/id, :name, :likes, {:friends [:db/id :name]}] 1) ; => {:db/id   1,
                                                                                                       :name    \"Ivan\"
                                                                                                       :likes   [:pizza]
                                                                                                       :friends [{:db/id 2, :name \"Oleg\"}]}

                  The arity-2 version takes :selector and :eid in arg-map."}
  pull
  c/pull)

(def ^{:arglists '([conn arg-map][conn selector eids][conn selector eids db-tx])
       :doc      "Same as [[pull]], but accepts sequence of ids and returns sequence of maps.

                  Usage:

                      (pull-many db [:db/id :name] [1 2]) ; => [{:db/id 1, :name \"Ivan\"}
                                                                {:db/id 2, :name \"Oleg\"}]"}
  pull-many
  c/pull-many)

(def ^{:arglists '([conn query & args] [conn arg-map])
       :doc "Executes a datalog query. See [docs.datomic.com/on-prem/query.html](https://docs.datomic.com/on-prem/query.html).

             Usage:

             Query as parameter with additional args:

                 (q '[:find ?value
                      :where [_ :likes ?value]]
                    #{[1 :likes \"fries\"]
                      [2 :likes \"candy\"]
                      [3 :likes \"pie\"]
                      [4 :likes \"pizza\"]}) ; => #{[\"fries\"] [\"candy\"] [\"pie\"] [\"pizza\"]}

             Or query passed in arg-map:

                 (q {:query '[:find ?value
                              :where [_ :likes ?value]]
                     :offset 2
                     :limit 1
                     :args [#{[1 :likes \"fries\"]
                              [2 :likes \"candy\"]
                              [3 :likes \"pie\"]
                              [4 :likes \"pizza\"]}]}) ; => #{[\"fries\"] [\"candy\"] [\"pie\"] [\"pizza\"]}

             Or query passed as map of vectors:

                 (q '{:find [?value] :where [[_ :likes ?value]]}
                    #{[1 :likes \"fries\"]
                      [2 :likes \"candy\"]
                      [3 :likes \"pie\"]
                      [4 :likes \"pizza\"]}) ; => #{[\"fries\"] [\"candy\"] [\"pie\"] [\"pizza\"]}

             Or query passed as string:

                 (q {:query \"[:find ?value :where [_ :likes ?value]]\"
                     :args [#{[1 :likes \"fries\"]
                              [2 :likes \"candy\"]
                              [3 :likes \"pie\"]
                              [4 :likes \"pizza\"]}]})

             Query passed as map needs vectors as values. Query can not be passed as list. The 1-arity function takes a map with the arguments :query and :args and optionally the additional keys :offset, :limit and :db-tx."}
  q
  c/q)

(defmulti datoms {:arglists '([conn arg-map] [conn index & components])
                  :doc "Index lookup. Returns a sequence of datoms (lazy iterator over actual DB index) which components
                        (e, a, v) match passed arguments. Datoms are sorted in index sort order. Possible `index` values
                        are: `:eavt`, `:aevt`, `:avet`.

                        Accepts conn and a map as arguments with the keys `:index` and `:components` provided within the
                        map, or the arguments provided separately.


                        Usage:

                        Set up your database. Beware that for the `:avet` index the index needs to be set to true for
                        the attribute `:likes`.

                            (d/transact conn [{:db/ident :name
                                               :db/type :db.type/string
                                               :db/cardinality :db.cardinality/one}
                                              {:db/ident :likes
                                               :db/type :db.type/string
                                               :db/index true
                                               :db/cardinality :db.cardinality/many}
                                              {:db/ident :friends
                                               :db/type :db.type/ref
                                               :db/cardinality :db.cardinality/many}]

                            (d/transact conn [{:db/id 4 :name \"Ivan\"
                                              {:db/id 4 :likes \"fries\"
                                              {:db/id 4 :likes \"pizza\"}
                                              {:db/id 4 :friends 5}])

                            (d/transact conn [{:db/id 5 :name \"Oleg\"}
                                              {:db/id 5 :likes \"candy\"}
                                              {:db/id 5 :likes \"pie\"}
                                              {:db/id 5 :likes \"pizza\"}])

                        Find all datoms for entity id == 1 (any attrs and values) sort by attribute, then value

                            (datoms conn {:index :eavt
                                          :components [1]}) ; => (#datahike/Datom [1 :friends 2]
                                                            ;     #datahike/Datom [1 :likes \"fries\"]
                                                            ;     #datahike/Datom [1 :likes \"pizza\"]
                                                            ;     #datahike/Datom [1 :name \"Ivan\"])

                        Find all datoms for entity id == 1 and attribute == :likes (any values) sorted by value

                            (datoms conn {:index :eavt
                                          :components [1 :likes]}) ; => (#datahike/Datom [1 :likes \"fries\"]
                                                                   ;     #datahike/Datom [1 :likes \"pizza\"])

                        Find all datoms for entity id == 1, attribute == :likes and value == \"pizza\"

                            (datoms conn {:index :eavt
                                          :components [1 :likes \"pizza\"]}) ; => (#datahike/Datom [1 :likes \"pizza\"])

                        Find all datoms for attribute == :likes (any entity ids and values) sorted by entity id, then value

                            (datoms conn {:index :aevt
                                          :components [:likes]}) ; => (#datahike/Datom [1 :likes \"fries\"]
                                                                 ;     #datahike/Datom [1 :likes \"pizza\"]
                                                                 ;     #datahike/Datom [2 :likes \"candy\"]
                                                                 ;     #datahike/Datom [2 :likes \"pie\"]
                                                                 ;     #datahike/Datom [2 :likes \"pizza\"])

                        Find all datoms that have attribute == `:likes` and value == `\"pizza\"` (any entity id)
                        `:likes` must be a unique attr, reference or marked as `:db/index true`

                            (datoms conn {:index :avet
                                          :components [:likes \"pizza\"]}) ; => (#datahike/Datom [1 :likes \"pizza\"]
                                                                           ;     #datahike/Datom [2 :likes \"pizza\"])

                        Find all datoms sorted by entity id, then attribute, then value

                            (datoms conn {:index :eavt}) ; => (...)


                        Useful patterns:

                        Get all values of :db.cardinality/many attribute

                            (->> (datoms conn {:index :eavt
                                               :components [eid attr]})
                                 (map :v))

                        Lookup entity ids by attribute value

                            (->> (datoms conn {:index :avet
                                               :components [attr value]})
                                 (map :e))

                        Find all entities with a specific attribute

                            (->> (datoms conn {:index :aevt
                                               :components [attr]})
                                 (map :e))

                        Find “singleton” entity by its attr

                            (->> (datoms conn {:index :aevt
                                               :components [attr]})
                                 first
                                 :e)

                        Find N entities with lowest attr value (e.g. 10 earliest posts)

                            (->> (datoms conn {:index :avet
                                               :components [attr]})
                                 (take N))

                        Find N entities with highest attr value (e.g. 10 latest posts)

                            (->> (datoms conn {:index :avet
                                               :components [attr]})
                                 (reverse)
                                 (take N))


                        Gotchas:

                        - Index lookup is usually more efficient than doing a query with a single clause.
                        - Resulting iterator is calculated in constant time and small constant memory overhead.
                        - Iterator supports efficient `first`, `next`, `reverse`, `seq` and is itself a sequence.
                        - Will not return datoms that are not part of the index (e.g. attributes with no `:db/index` in schema when querying `:avet` index).
                          - `:eavt` and `:aevt` contain all datoms.
                          - `:avet` only contains datoms for references, `:db/unique` and `:db/index` attributes."}
  (fn
    ([conn arg-map]
     (type arg-map))
    ([conn index & components]
     (type index))))

(defmethod datoms clojure.lang.PersistentArrayMap
  [conn {:keys [index components db-tx]}]
  (c/datoms conn index components db-tx))

(defmethod datoms clojure.lang.Keyword
  [conn index & components]
  {:pre [(keyword? index)]}
  (if (nil? components)
    (c/datoms conn index [])
    (c/datoms conn index components)))

#_(defmulti seek-datoms {:arglists '([db arg-map] [db index & components])
                         :doc "Similar to [[datoms]], but will return datoms starting from specified components and including rest of the database until the end of the index.

                             If no datom matches passed arguments exactly, iterator will start from first datom that could be considered “greater” in index order.

                             Usage:

                                 (seek-datoms @db {:index :eavt
                                                   :components [1]}) ; => (#datahike/Datom [1 :friends 2]
                                                                           #datahike/Datom [1 :likes \"fries\"]
                                                                           #datahike/Datom [1 :likes \"pizza\"]
                                                                           #datahike/Datom [1 :name \"Ivan\"]
                                                                           #datahike/Datom [2 :likes \"candy\"]
                                                                           #datahike/Datom [2 :likes \"pie\"]
                                                                           #datahike/Datom [2 :likes \"pizza\"])

                                 (seek-datoms @db {:index :eavt
                                                   :components [1 :name]}) ; => (#datahike/Datom [1 :name \"Ivan\"]
                                                                                 #datahike/Datom [2 :likes \"candy\"]
                                                                                 #datahike/Datom [2 :likes \"pie\"]
                                                                                 #datahike/Datom [2 :likes \"pizza\"])

                                 (seek-datoms @db {:index :eavt
                                                   :components [2]}) ; => (#datahike/Datom [2 :likes \"candy\"]
                                                                           #datahike/Datom [2 :likes \"pie\"]
                                                                           #datahike/Datom [2 :likes \"pizza\"])

                             No datom `[2 :likes \"fish\"]`, so starts with one immediately following such in index

                                 (seek-datoms @db {:index :eavt
                                                   :components [2 :likes \"fish\"]}) ; => (#datahike/Datom [2 :likes \"pie\"]
                                                                                           #datahike/Datom [2 :likes \"pizza\"])"}
    (fn
      ([db arg-map]
       (type arg-map))
      ([db index & components]
       (type index))))

#_(defmethod seek-datoms clojure.lang.PersistentArrayMap
    [db {:keys [index components]}]
    {:pre [(db/db? db)]}
    (c/seek-datoms db index components))

#_(defmethod seek-datoms clojure.lang.Keyword
    [db index & components]
    {:pre [(db/db? db)
           (keyword? index)]}
    (if (nil? components)
      (c/seek-datoms db index [])
      (c/seek-datoms db index components)))

#_(def ^:private last-tempid (atom -1000000))

#_(def ^{:arglists '([part] [part x])
         :doc "Allocates and returns a unique temporary id (a negative integer). Ignores `part`. Returns `x` if it is specified.

             Exists for Datomic API compatibility. Prefer using negative integers directly if possible."}
    tempid
    c/tempid)

#_(def ^{:arglists '([db eid])
         :doc      "Retrieves an entity by its id from database. Entities are lazy map-like structures to navigate DataScript database content.

                  For `eid` pass entity id or lookup attr:

                      (entity db 1)
                      (entity db [:unique-attr :value])

                  If entity does not exist, `nil` is returned:

                      (entity db -1) ; => nil

                  Creating an entity by id is very cheap, almost no-op, as attr access is on-demand:

                      (entity db 1) ; => {:db/id 1}

                  Entity attributes can be lazily accessed through key lookups:

                      (:attr (entity db 1)) ; => :value
                      (get (entity db 1) :attr) ; => :value

                  Cardinality many attributes are returned sequences:

                      (:attrs (entity db 1)) ; => [:v1 :v2 :v3]

                  Reference attributes are returned as another entities:

                      (:ref (entity db 1)) ; => {:db/id 2}
                      (:ns/ref (entity db 1)) ; => {:db/id 2}

                  References can be walked backwards by prepending `_` to name part of an attribute:

                      (:_ref (entity db 2)) ; => [{:db/id 1}]
                      (:ns/_ref (entity db 2)) ; => [{:db/id 1}]

                  Reverse reference lookup returns sequence of entities unless attribute is marked as `:db/component`:

                      (:_component-ref (entity db 2)) ; => {:db/id 1}

                  Entity gotchas:

                  - Entities print as map, but are not exactly maps (they have compatible get interface though).
                  - Entities are effectively immutable “views” into a particular version of a database.
                  - Entities retain reference to the whole database.
                  - You can’t change database through entities, only read.
                  - Creating an entity by id is very cheap, almost no-op (attributes are looked up on demand).
                  - Comparing entities just compares their ids. Be careful when comparing entities taken from differenct dbs or from different versions of the same db.
                  - Accessed entity attributes are cached on entity itself (except backward references).
                  - When printing, only cached attributes (the ones you have accessed before) are printed. See [[touch]]."}
    entity
    c/entity)

(def ^{:arglists '([conn])
       :doc "Returns the underlying immutable database value from a connection.

             Exists for Datomic API compatibility. Prefer using `@conn` directly if possible."}
  db
  c/db)
