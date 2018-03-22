(ns cc.controller
  (:require [cc.config :as config]
            [cc.chain]
            [cheshire.core :as json]
            [taoensso.timbre :as log :refer [error info]]
            [clojure.java.io :as io]))

(defn new-transaction [{:keys [body] :as request}]
  (with-open [rdr (io/reader body)]
    (let [jbody (json/parse-stream rdr true)
          sender (:sender jbody)
          recipient (:recipient jbody)
          amount (:amount jbody)]

      (swap! cc.chain/chain-db
             cc.chain/new-transaction
             sender
             recipient
             amount)

      (let [index (+ 1 (:index (cc.chain/last-block @cc.chain/chain-db)))
            response-body (json/encode {:message (str "Transaction will be added to block " index)})]

        {:status 201
         :headers {"Content-Type" "application/json"}
         :body response-body}))))

(defn mine [request]
  (let [last-block (cc.chain/last-block @cc.chain/chain-db)
        last-proof (:proof last-block)
        proof (cc.chain/proof-of-work last-proof)]

    (swap! cc.chain/chain-db
           cc.chain/new-transaction
           "0"            ;; sender
           "this node!!!" ;; recipient, send to self
           1              ;; amount
)

    (let [previous-hash (cc.chain/hash-sha256 (pr-str last-block))
          new-blockchain (swap! cc.chain/chain-db
                                cc.chain/new-block
                                proof
                                previous-hash)

          block (cc.chain/last-block new-blockchain)

          response-body (json/encode {:message "new block forged"
                                      :index (:index block)
                                      :transactions (:transactions block)
                                      :proof (:proof block)
                                      :previous-hash (:previous-hash block)})]

      {:status 200
       :headers {"Content-Type" "application/json"}
       :body response-body})))

(defn full-chain [request]
  {:status 200
   :headers {"Content-Type" "application/json"}
   :body (json/encode {:chain (:chain @cc.chain/chain-db)
                       :length (count (:chain @cc.chain/chain-db))})})
