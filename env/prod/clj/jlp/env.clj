(ns jlp.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[jlp started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[jlp has shut down successfully]=-"))
   :middleware identity})
