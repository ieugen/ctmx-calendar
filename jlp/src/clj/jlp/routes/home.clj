(ns jlp.routes.home
  (:require
    [ctmx.core :as ctmx]
    [ctmx.render :as render]
    [hiccup.page :refer [html5]]))

(defn html-response [body]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body body})

(defn html5-response
  ([body]
   (html-response
    (html5
     [:head
      [:meta {:name "viewport"
              :content "width=device-width, initial-scale=1, shrink-to-fit=no"}]]
     [:body (render/walk-attrs body)]
     [:script {:src "https://unpkg.com/htmx.org@1.5.0"}]))))

(defn parse-int [number-string]
  (try (Integer/parseInt number-string)
       (catch Exception e nil)))

(ctmx/defcomponent ^:endpoint root [req]
  ;; (tap> req)
  (let [num-clicks (-> req :form-params (get "num-clicks"))
        parsed (parse-int num-clicks)
        num-clicks (if parsed parsed 0)]
    ;; (tap> (str "parsed" num-clicks " " parsed) )
    [:div.m-3 {:hx-post "root"
               :hx-swap "outerHTML"
               :hx-vals {:num-clicks (inc num-clicks)}}
     "You have clicked me " num-clicks " times."]))

;; using ^:int num-clicks fails
(ctmx/defcomponent ^:endpoint npe [req ^:long num-clicks]
  (tap> (str "parsed " num-clicks))
  [:div.m-3 {:hx-post "npe"
             :hx-swap "outerHTML"
             :hx-vals {:num-clicks (inc num-clicks)}}
   "You have clicked me " num-clicks " times."])

(defn home-routes []
  [(ctmx/make-routes
    "/"
    (fn [req]
      (html5-response
       (root req))))
   (ctmx/make-routes
    "/npe"
    (fn [req]
      (html5-response
       (npe req "0"))))])
