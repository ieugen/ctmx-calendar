(ns ctmx-calendar.routes.calendar
  (:require [clojure.string :as str]
            [ctmx.core :as ctmx]
            [ctmx.render :as render]
            [hiccup.page :refer [html5]]
            [hyperfiddle.rcf :refer [tests]])
  (:import (java.time DayOfWeek LocalDate Month)
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

(defn local-day-name
  ([^:int day]
   (local-day-name day RO))
  ([^:int day ^:Locale locale]
   (-> day
       (DayOfWeek/of)
       (.getDisplayName TextStyle/FULL locale)
       (str/capitalize))))

(defn ->day-names
  [^:Locale locale]
  (into [] (map #(local-day-name % locale) (range 1 8))))

(defn local-month-name
  ([^:int month]
   (local-month-name month RO))
  ([^:int month ^:Locale locale]
   (-> month
       (Month/of)
       (.getDisplayName TextStyle/FULL locale)
       (str/capitalize))))

(defn ->month-names
  [^:Locale locale]
  (into [] (map #(local-month-name % locale) (range 1 13))))

(def day-names (->day-names RO))

(defn date->day-of-week
  "Reurn the day of the week for the given date."
  ^:int [^:LocalDate first-day-date]
  (-> first-day-date
      (DayOfWeek/from)
      (.getValue)))

(tests
 "working with dates"

 (local-day-name 1) := "Luni"
 (local-day-name 1 Locale/FRENCH) := "Lundi"

 (->day-names RO) := ["Luni" "Marți" "Miercuri" "Joi" "Vineri" "Sâmbătă" "Duminică"]

 (local-month-name 1) := "Ianuarie"
 (local-month-name 1 Locale/UK) := "January"

 (let [m (->month-names RO)]
   (count m) := 12
   (first m) := "Ianuarie")

 (date->day-of-week (LocalDate/of 2022 8 12)) := 5
 (date->day-of-week (LocalDate/of 2022 8 13)) := 6
 
 0)

(defn get-calendar-rows
  ;; port of Port of https://github.com/rajasegar/htmx-calendar/blob/9aa49d53730bae603eace917649cb212499e9db0/index.js#L32   
  [year month]
  (let [first-day-date (LocalDate/of year month 1)
        days-in-month (->> first-day-date .lengthOfMonth)
        first-day (date->day-of-week first-day-date)
        month-start-offset (- first-day 1)
        days (concat (repeat month-start-offset 0)
                     (range 1 (inc days-in-month)))
        weeks-as-raws (partition 7 7 (repeat 0) days)]
    weeks-as-raws))

(tests
 "calendar rows"

 (get-calendar-rows 2022 8) := '((1 2 3 4 5 6 7)
                                 (8 9 10 11 12 13 14)
                                 (15 16 17 18 19 20 21)
                                 (22 23 24 25 26 27 28)
                                 (29 30 31 0 0 0 0))

 (get-calendar-rows 2022 9) := '((0 0 0 1 2 3 4)
                                 (5 6 7 8 9 10 11)
                                 (12 13 14 15 16 17 18)
                                 (19 20 21 22 23 24 25)
                                 (26 27 28 29 30 0 0))

 (get-calendar-rows 2024 2) := '((0 0 0 1 2 3 4)
                                 (5 6 7 8 9 10 11)
                                 (12 13 14 15 16 17 18)
                                 (19 20 21 22 23 24 25)
                                 (26 27 28 29 0 0 0))

 0)

(ctmx/defcomponent ^:endpoint next-month [req]
  (let [now (LocalDate/now)]
    (println "neeeext")
    (-> now (.plusDays 1) str)))

(ctmx/defcomponent ^:endpoint previous-month [req]
  (let [now (LocalDate/now)]
    (println "previous")
    (-> now (.plusDays -1) str)))

(ctmx/defcomponent ^:endpoint modal [req]
  (let [now (LocalDate/now)]
    (println "modal")
    (-> now (.plusDays 1) str)))

(ctmx/defcomponent ^:endpoint today [req]
  (let [now (LocalDate/now)]
    (println "today")
    (-> now (.plusDays 1) str)))

(ctmx/defcomponent ^:endpoint calendar [req state]
  ;; we do not use next and previous within calendar
  ;; but we want the endpoints to be exposed so we reference them here
  next-month
  previous-month
  modal
  today
  (let [today (:today @state)
        date 9
        month 8
        year 2022
        current-month 8
        current-year 2022]
    (list
     [:div.d-flex.justify-content-between.align-items-center
      [:h2#current-month "August - 2022"]
      [:div.text-success {:hidden "" :data-activity-indicator ""}
       [:div.spinner-border.spinner-border-sm]
       [:span "Loading..."]]
      [:div
       [:button.btn.btn-secondary.me-2.btn-sm {:type "button"
                                               :hx-get "today"
                                               :hx-target "#calendar"
                                               :data-bs-toggle "tooltip"
                                               :data-bs-placement "top"
                                               :title today} "Today"]

       [:button.btn.btn-secondary.ms-2.btn-sm {:type "button"
                                               :hx-get "previous-month"
                                               :hx-target "#calendar"
                                               :data-bs-toggle "tooltip"
                                               :data-bs-placement "top"
                                               :title "Previous Month"}
        [:img {:src "/img/chevron-left.svg"}]]

       [:button.btn.btn-secondary.ms-2.btn-sm {:type "button"
                                               :hx-get "next-month"
                                               :hx-target "#calendar"
                                               :data-bs-toggle "tooltip"
                                               :data-bs-placement "top"
                                               :title "Next Month"}
        [:img {:src "/img/chevron-right.svg"}]]]]

     [:table.table.table-bordered.mt-2 {:id "calendar-"}
      [:thead.table-dark
       [:tr.text-center
        (map (fn [e] [:th {:style "width:14%"} e]) day-names)]]
      [:tbody (for [row (get-calendar-rows 2022 8)]
                [:tr.table-light
                 (for [col row]
                   [:td {:id (str "date-" col "-" current-month "-" current-year)
                         :class (when (and (= col date)
                                           (= month current-month)
                                           (= year current-year))
                                  "table-info")
                         :hx-get (str "modal?date=" col "-" current-month "-" current-year)
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

(defn calendar-routes []
  (let [state (atom {})]
    (ctmx/make-routes
     "/"
     (fn [req]
       (html5-response
        (calendar req state))))))

(comment

  (clojure.pprint/pprint
   (ctmx/make-routes
    "/test"
    (fn [req]
      (html5-response
       (calendar req (atom {}))))))

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

  0)
