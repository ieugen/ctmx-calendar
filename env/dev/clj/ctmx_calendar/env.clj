(ns ctmx-calendar.env
  (:require
    [selmer.parser :as parser]
    [clojure.tools.logging :as log]
    [ctmx-calendar.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[jlp started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[jlp has shut down successfully]=-"))
   :middleware wrap-dev})
