(ns auaupi.routes
  (:require
   [io.pedestal.http :as http]
   [io.pedestal.interceptor :as io]
   [pedestal-api
    [core :as api]
    [helpers :refer [before defbefore defhandler handler]]]
   [route-swagger.doc :as sw.doc]
   [auaupi.not-logic :as not-logic]
   [auaupi.datomic :as datomic]
   [auaupi.logic :as logic]
   [auaupi.config :as config]
   [schema.core :as s]))

(s/defschema Dog
  {:id s/Int
   :name s/Str
   :breed s/Str
   :image s/Str
   :birth s/Str
   :gender (s/enum "m" "f")
   :port (s/enum "p" "m" "g")
   :castrated? s/Bool
   :adopted? s/Bool})

(defn get-dogs [ctx]
  (let [req (get ctx :request)
        result (-> req
                   :params
                   (not-logic/check-params!
                    (datomic/open-connection config/config-map))
                   http/json-response)]
    (assoc ctx :response result)))

(defn get-dog-by-id [ctx]
  (let [req (get ctx :request)
        result (-> req
                   :path-params
                   :id
                   Long/valueOf
                   (datomic/find-dog-by-id
                    (datomic/open-connection config/config-map))
                   logic/datom->dog-full
                   logic/data->response)]
    (assoc ctx :response result)))

(defn post-dogs [ctx]
  (let [req (get ctx :request)
        result (http/json-response {:status 200 :body "Registered Dog"})]
    (-> req
        (not-logic/check-breed! config/config-map)
        (not-logic/valid-dog! config/config-map)
        (datomic/transact-dog! config/config-map))
    (assoc ctx :response result)))

(defn post-adoption [ctx]
  (let [req (get ctx :request)
        result (-> req
                   :path-params
                   :id
                   Long/valueOf
                   (not-logic/check-adopted!
                    (datomic/open-connection config/config-map)))]
    (assoc ctx :response result)))

(def list-dogs-route
  (sw.doc/annotate
   {:summary    "List all dogs available for adoption"
    :parameters {:query-params {(s/optional-key :breed) s/Str
                                :castrated? s/Bool
                                (s/optional-key :name) s/Str
                                :port (s/enum "p" "g" "m")
                                :gender (s/enum "f" "m")
                                (s/optional-key :birth) s/Str}}
    :responses  {200 {:body s/Str}
                 400 {:body s/Str}}
    :operationId ::list-dogs}
   (io/interceptor 
    {:name  :response-dogs 
     :enter get-dogs})))

(def get-dog-route
  (sw.doc/annotate
   {:summary    "List all dogs available for adoption"
    :parameters {:path-params {:id s/Int}}
    :responses  {200 {:body Dog}
                 400 {:body s/Str}}
    :operationId ::specific-dog}
   (io/interceptor
    {:name  ::response-specific-dog
     :enter get-dog-by-id})))

(def post-dog-route
  (sw.doc/annotate
   {:summary     "Add a dog to our adoption list"
    :parameters  {:body-params Dog}
    :responses   {201 {:body s/Str}}
    :operationId ::create-dog}
   (io/interceptor
    {:name ::post-dogs
     :enter post-dogs})))

(def adopt-dog-route
  (sw.doc/annotate
   {:summary     "Adopt a dog"
    :parameters  {:path-params {:id s/Int}}
    :responses   {200 {:body s/Str}}
    :operationId ::adopt-dog}
   (io/interceptor
    {:name ::adopt-dog
     :enter post-adoption})))