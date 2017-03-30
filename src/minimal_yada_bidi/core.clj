(ns minimal-yada-bidi.core
  (:require
    [yada.yada :as yada]))

; simple atom for exposing a global function so the server can close itself
(def closer (atom nil))

(defn the-routes []
  ["/"
   {
    "hello" (yada/as-resource "Hello World!")
    "die"   (yada/as-resource (fn []
                                (future (Thread/sleep 100) (@closer))
                                "shutting down in 100ms..."))}])

(defn run-server-returning-promise []
  (let [listener (yada/listener (the-routes) {:port 3000})
        done-promise (promise)]
    (reset! closer (fn []
                     ((:close listener))
                     (deliver done-promise :done)
                     ))
    done-promise))

(defn -main
  [& args]
  (let [done (run-server-returning-promise)]
    (println "server running on port 3000... GET \"http://localhost/die\" to kill")
    @done))

(comment "to run in a repl, eval this:"
         (def server-promise (run-server-returning-promise))
         "then either wait on the promise:"
          @server-promise
         "or with a timeout"
         (deref server-promise 1000 :timeout)
         "or close it yourself"
         (@closer)
         )
