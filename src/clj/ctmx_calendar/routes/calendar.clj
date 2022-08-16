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

(defn day-name
  ([^:int day]
   (day-name day RO))
  ([^:int day ^:Locale locale]
   (-> day
       (DayOfWeek/of)
       (.getDisplayName TextStyle/FULL locale)
       (str/capitalize))))

(defn ->day-names
  [^:Locale locale]
  (into [] (map #(day-name % locale) (range 1 8))))

(defn month-name
  ([^:int month]
   (month-name month RO))
  ([^:int month ^:Locale locale]
   (-> month
       (Month/of)
       (.getDisplayName TextStyle/FULL locale)
       (str/capitalize))))

(defn ->month-names
  [^:Locale locale]
  (into [] (map #(month-name % locale) (range 1 13))))

(def day-names (->day-names RO))

(defn date->day-of-week
  "Reurn the day of the week for the given date."
  ^:int [^:LocalDate first-day-date]
  (-> first-day-date
      (DayOfWeek/from)
      (.getValue)))

(tests
 "working with dates"

 (day-name 1) := "Luni"
 (day-name 1 Locale/FRENCH) := "Lundi"

 (->day-names RO) := ["Luni" "Marți" "Miercuri" "Joi" "Vineri" "Sâmbătă" "Duminică"]

 (month-name 1) := "Ianuarie"
 (month-name 1 Locale/UK) := "January"

 (let [m (->month-names RO)]
   (count m) := 12
   (first m) := "Ianuarie")

 (date->day-of-week (LocalDate/of 2022 8 12)) := 5
 (date->day-of-week (LocalDate/of 2022 8 13)) := 6
 
 0)

(def STATE (atom {}))

(defn initialize-state!
  [state & {:keys [^:LocalDate today] :as _opts}]
  (let [now (or today (LocalDate/now))
        current-month (-> now .getMonthValue)
        current-year (-> now .getYear)]
    (swap! state merge {:current-month current-month
                        :current-year current-year})
    @state))

;; initialize state in the app
(initialize-state! STATE)

(tests
 "initialize state"

 (let [d (LocalDate/of 2019 10 10)]
   (initialize-state! (atom {}) :today d) := {:current-year 2019
                                              :current-month 10}))


(defn get-calendar-rows
  ;; port of Port of https://github.com/rajasegar/htmx-calendar/blob/9aa49d53730bae603eace917649cb212499e9db0/index.js#L32   
  [^:int year ^:int month]
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

(defn calendar-markup [current-year current-month]
  (let [month-year (str (month-name current-month) " - " current-year)
        today (LocalDate/now)
        date (-> today .getDayOfMonth)
        month (-> today .getMonthValue)
        year (-> today .getYear)
        rows (get-calendar-rows current-year current-month)
        td-id (fn [col] (str "date-" col "-" current-month "-" current-year))
        col->date-str (fn [col] (str current-year "-" current-month "-" col))
        is-current-date? (fn [col] (and (= col date)
                                        (= month current-month)
                                        (= year current-year)))]
    (list
     [:div.d-flex.justify-content-between.align-items-center
      [:h2#current-month month-year]
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
      [:tbody (for [row rows]
                [:tr.table-light
                 (for [col row]
                   [:td {:id (td-id col)
                         :class (when (is-current-date? col)
                                  "table-info")
                         :hx-get (str "modal?date=" (col->date-str col))
                         :hx-target "#modals-here"
                         :hx-trigger "click"
                         :_ "on htmx:afterLoad
                             wait 10ms
                             then add .show to #modal
                             then add .show to #modal-backdrop"}
                    [:span (if (= col 0) "" col)]
                    [:div.mt-2 {:id (str "events-" (col->date-str col))
                                :style "height:65px;overflow-y:auto"}]])])]]
     [:div#modals-here])))

(ctmx/defcomponent ^:endpoint next-month [req]
  (let [{:keys [current-month current-year]} @STATE
        date (LocalDate/of current-year current-month 1)
        date (-> date (.plusMonths 1))
        current-month (-> date (.getMonthValue))
        current-year (-> date (.getYear))]
    (swap! STATE assoc
           :current-month current-month
           :current-year current-year)
    (calendar-markup current-year current-month)))

(ctmx/defcomponent ^:endpoint previous-month [req]
  (let [{:keys [current-month current-year]} @STATE
        date (LocalDate/of current-year current-month 1)
        date (-> date (.minusMonths 1))
        current-month (-> date (.getMonthValue))
        current-year (-> date (.getYear))]
    (swap! STATE assoc
           :current-month current-month
           :current-year current-year)
    (calendar-markup current-year current-month)))

(ctmx/defcomponent ^:endpoint modal [req]
  (let [now (LocalDate/now)]
    (println "modal")
    (-> now (.plusDays 1) str)))

(ctmx/defcomponent ^:endpoint today [req]
  (let [date (LocalDate/now)
        current-month (-> date (.getMonthValue))
        current-year (-> date (.getYear))]
    (swap! STATE assoc
           :current-month current-month
           :current-year current-year)
    (calendar-markup current-year current-month)))

(ctmx/defcomponent ^:endpoint calendar [req]
  ;; we do not use next and previous within calendar
  ;; but we want the endpoints to be exposed so we reference them here
  next-month
  previous-month
  modal
  today
  (let [{:keys [current-month current-year]} @STATE]
    (calendar-markup current-year current-month)))

(defn calendar-routes []
  (ctmx/make-routes
   "/"
   (fn [req]
     (html5-response
      (calendar req)))))

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
