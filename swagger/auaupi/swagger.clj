(ns auaupi.swagger
  (:require [io.pedestal.http :as http]
            [io.pedestal.test :as p.test]
            [clojure.java.io :as io]
            [auaupi.service :as service]))

(defn response-swagger []
  (let [service (::http/service-fn (http/create-server service/pedestal-config))
        response (-> service
                     (p.test/response-for :get "/auaupi/swagger.json"))]
      response))

(defn -main []
  (loop []
    (if (not (= (:status (response-swagger)) 404))
      (do
        (println "deu certo")
        (io/make-parents "doc/swagger/swagger.json")
        (spit "doc/swagger/swagger.json" (:body (response-swagger))))
      (recur)))
  (shutdown-agents)
  (System/exit 0))