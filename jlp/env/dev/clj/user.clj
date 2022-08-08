(ns user
  "Userspace functions you can run by default in your local REPL."
  (:require
   [jlp.config :refer [env]]
    [clojure.pprint]
    [clojure.spec.alpha :as s]
    [expound.alpha :as expound]
    [mount.core :as mount]
    [jlp.core :refer [start-app]]))

(alter-var-root #'s/*explain-out* (constantly expound/printer))

(add-tap (bound-fn* clojure.pprint/pprint))

;; (require '[portal.api :as p])
;; ;; or with an extension installed, do:
;; (def p (p/open {:launcher :vs-code}))  ; JVM only for now

;; (add-tap #'p/submit) ; Add portal as a tap> target

(defn start
  "Starts application.
  You'll usually want to run this on startup."
  []
  (mount/start-without #'jlp.core/repl-server))

(defn stop
  "Stops application."
  []
  (mount/stop-except #'jlp.core/repl-server))

(defn restart
  "Restarts application."
  []
  (stop)
  (start))


