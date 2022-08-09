(ns jlp.routes.calendar
  (:require [clojure.string :as str]
            [ctmx.core :as ctmx] 
            [ctmx.rt :as rt]
            [ctmx.render :as render]
            [hiccup.page :refer [html5]])
  (:import (java.time DayOfWeek
                      LocalDate)
           (java.time.format TextStyle)
           (java.time.temporal TemporalAdjusters)
           (java.util Locale)))

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
              :content "width=device-width, initial-scale=1, shrink-to-fit=no"}]
      [:link {:href "https://cdn.jsdelivr.net/npm/bootstrap@5.2.0/dist/css/bootstrap.min.css"
              :rel "stylesheet"
              :integrity "sha256-7ZWbZUAi97rkirk4DcEp4GWDPkWpRMcNaEyXGsNXjLg="
              :crossorigin "anonymous"}]]
     [:body
      [:div.container
       [:div.card.mt-3
        [:div.card-header
         [:h1 "Calendar"]]
        [:div#calendar.card-body
         (render/walk-attrs body)]]]]
     [:script {:src "https://unpkg.com/htmx.org@1.8.0"}]
     [:script {:src "https://unpkg.com/hyperscript.org@0.9.7"}]
     [:script {:src "https://cdn.jsdelivr.net/npm/bootstrap@5.2.0/dist/js/bootstrap.bundle.min.js"
               :integrity "sha256-wMCQIK229gKxbUg3QWa544ypI4OoFlC2qQl8Q8xD8x8="
               :crossorigin "anonymous"}]))))

(def RO (Locale. "ro"))

(defn day->localized-name
  ([day]
   (day->localized-name day RO))
  ([day ^:Locale locale]
   (-> day
       (DayOfWeek/of)
       (.getDisplayName TextStyle/FULL locale)
       (str/capitalize))))

(defn ->day-names 
  [^:Locale locale]
  (into [] (map #(day->localized-name % locale) (range 1 8))))

(def day-names (->day-names RO))

(defn get-calendar-rows
  "Port of 
   https://github.com/rajasegar/htmx-calendar/blob/9aa49d53730bae603eace917649cb212499e9db0/index.js#L32"
  [month year]
  (let [today (LocalDate/now)
        days-in-month (->> today .lengthOfMonth)
        first-day (-> today
                      (.with (TemporalAdjusters/firstDayOfMonth))
                      (DayOfWeek/from)
                      (.getValue))
        month-start-offset (- first-day 1)
        days (concat (repeat month-start-offset 0)
                     (range 1 (inc days-in-month)))
        weeks-as-raws (partition 7 7 (repeat 0) days)]
    weeks-as-raws))

(comment

  (let [today (LocalDate/now)
        days-in-month (->> today .lengthOfMonth)
        first-day (-> today
                      (.with (TemporalAdjusters/firstDayOfMonth))
                      (DayOfWeek/from)
                      (.getValue))
        month-start-offset (- first-day 1)
        days (concat (repeat month-start-offset 0)
                     (range 1 (inc days-in-month)))
        weeks-as-raws (partition 7 7 (repeat 0) days)])

  (-> DayOfWeek/SUNDAY (.getValue))

  (doall (repeat 7 0))
  
  (-> (LocalDate/now) (.minusDays 1))

  (->> (LocalDate/now) .lengthOfMonth)

  
  (-> (LocalDate/now) (.getMonth))
  (-> (LocalDate/now)
      (DayOfWeek/from)
      (.getDisplayName TextStyle/FULL (Locale. "ro"))
      (str/capitalize))

  (-> (LocalDate/now)
      (.with (TemporalAdjusters/firstDayOfMonth))
      (DayOfWeek/from)
      (.getValue))

  (-> (LocalDate/now)
      (.with (TemporalAdjusters/firstDayOfMonth)))
  )

(ctmx/defcomponent ^:endpoint calendar [req]
  (let [today (LocalDate/now)
        date 9
        month 8
        year 2022
        current-month 8
        current-year 2022]
    (list
     [:div.d-flex.justify-content-between.align-items-center
      [:h2#current-month "August - 2022 {monthYear}"]
      [:ul [:li "Calendar cu necesarul de oameni pe toate locațiile ?!"]]
      [:div.text-success {:hidden "" :data-activity-indicator ""}
       [:div.spinner-border.spinner-border-sm]
       [:span "Loading..."]]
      [:div
       [:button.btn.btn-secondary.me-2.btn-sm {:type "button"
                                               :hx-get "/today"
                                               :hx-target "#calendar"
                                               :data-bs-toggle "tooltip"
                                               :data-bs-placement "top"
                                               :title today} "Today"]

       [:button.btn.btn-secondary.ms-2.btn-sm {:type "button"
                                               :hx-get "/previous"
                                               :hx-target "#calendar"
                                               :data-bs-toggle "tooltip"
                                               :data-bs-placement "top"
                                               :title "Previous Month"}
        [:img {:src "/img/chevron-left.svg"}]]

       [:button.btn.btn-secondary.ms-2.btn-sm {:type "button"
                                               :hx-get "/next"
                                               :hx-target "#calendar"
                                               :data-bs-toggle "tooltip"
                                               :data-bs-placement "top"
                                               :title "Next Month"}
        [:img {:src "/img/chevron-right.svg"}]]]]

     [:table.table.table-bordered.mt-2 {:id "calendar-"}
      [:thead.table-dark
       [:tr.text-center
        (map (fn [e] [:th {:style "width:14%"} e]) day-names)]]
      [:tbody (for [row (get-calendar-rows nil nil)]
                [:tr.table-light
                 (for [col row]
                   [:td {:id (str "date-" col "-" current-month "-" current-year)
                         :class (when (and (= col date)
                                           (= month current-month)
                                           (= year current-year))
                                  "table-info")
                         :hx-get (str "/modal?date=" col "-" current-month "-" current-year)
                         :hx-target "#modals-here"
                         :hx-trigger "click"
                         :_ "on htmx:afterLoad 
                             wait 10ms 
                             then add .show to #modal 
                             then add .show to #modal-backdrop"}
                    [:span (if (= col 0) "" col)]
                    [:div.mt-2 {:id (str "events-" col "-" current-month "-" current-year)
                                :style "height:65px;overflow-y:auto"}]])])]]
     [:div#modals-here])))


(ctmx/defcomponent ^:endpoint markup [req]
                   )


(defn calendar-routes []
  (conj )(ctmx/make-routes
   "/"
   (fn [req]
     (html5-response
      (calendar req))))
  [(ctmx/make-routes
    "/next"
    (fn [req]
      (markup req (-> (LocalDate/now) (.plusDays 1)))))
   (ctmx/make-routes
    "/previous"
    (fn [req]
      (markup req (-> (LocalDate/now) (.minusDays 1)))))])


(comment

(require '[clojure.pprint :as p])
  
  (p/pprint (ctmx/make-routes
             "/"
             (fn [req]
               (html5-response
                (calendar req)))))
  
  
  (p/pprint (conj (rest (ctmx/make-routes
                         "/"
                         (fn [req]
                           (html5-response
                            (calendar req)))))
                  (rest (ctmx/make-routes
                         "/next"
                         (fn [req]
                           (html5-response
                            (markup req)))))))
  
  
  0)