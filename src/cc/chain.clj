(ns cc.chain)

(defn hash-sha256 [string]
  (let [sha256-instance (com.google.common.hash.Hashing/sha256)]
    (.toString (.hashString sha256-instance string com.google.common.base.Charsets/UTF_8))))

(defn new-block [chain proof previous-hash]
  (let [block {:index (+ (count (:chain chain)) 1)
               :timetamp (-> java.time.ZoneOffset/UTC
                             java.time.ZonedDateTime/now
                             .toInstant
                             .toEpochMilli)
               :transactions (:transactions chain)
               :proof proof
               :previous-hash (or previous-hash (hash-sha256 (last (:chain chain))))}]

    (-> chain
        (update :chain #(conj % block))
        (assoc :transactions []))))

(defn new-chain []
  (new-block {:chain []
              :transactions []}
             100
             1))

(defn new-transaction [chain sender recipient amount]
  (let [transaction {:sender sender
                     :recipient recipient
                     :amount amount}]

    (update chain :transactions #(conj % transaction))))

(defn valid-proof [last-proof proof]
  (let [guess (str last-proof proof)
        guess-hash (hash-sha256 guess)]
    (clojure.string/starts-with? guess-hash "0000")))

(defn proof-of-work [last-proof]
  (loop [proof 0]
    (if (not (valid-proof last-proof proof))
      (recur (inc proof))
      proof)))

(defn last-block [{:keys [chain]}]
  (last chain))

(defonce chain-db (atom (new-chain)))
