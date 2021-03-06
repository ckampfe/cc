(ns cc.web
  (:require [aleph.http :as http]
            [bidi.ring :as bidi-ring]
            [mount.core :refer [defstate]]
            [ring.middleware.defaults :refer [api-defaults wrap-defaults]]
            [cc.config :as config]
            [cc.controller :as controller]))

(def routes ["/" {"transactions/new" {:post controller/new-transaction}
                  "mine" {:get controller/mine}
                  "chain" {:get controller/full-chain}}])

(def handler
  (-> routes
      bidi-ring/make-handler
      (wrap-defaults api-defaults)))

(defn start-server!
  "Starts an HTTP server using the provided Ring `handler`."
  [config]
  (http/start-server handler {:port (config/webserver-port config)}))

(defstate server
  :start (start-server! config/config)
  :stop (.close server))
