(ns ctmx-calendar.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[cmtx-calendar started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[cmtx-calendar has shut down successfully]=-"))
   :middleware identity})
