(ns minimal-yada-bidi.core
  (:require
    [yada.yada :as yada]
    [schema.core :as s]))

; simple atom for exposing a global function so the server can close itself
(def closer (atom nil))

(defn the-widget-resource [initial-value]
  "simple modifiable widget. look at yada.resources.atom-resource for ways to add last-modified headers and the like"
  (let [value (atom initial-value)]
    (yada/resource
      {:methods {:get  {:produces "application/json"
                        :response (fn [ctx] @value)}
                 :post {:parameters {:body {:sprockets               s/Num
                                            :reaction                s/Str
                                            (s/optional-key :extras) s/Any}}
                        :consumes   "application/json"
                        :response   (fn [ctx]
                                      (let [body (get-in ctx [:parameters :body])]
                                        (reset! value body)))}}})))

(defn the-routes []
  ["/"
   {
    "hello"             (yada/as-resource "Hello World!")
    "json"              (yada/as-resource {:message "yo!"})
    "modifiable-string" (yada/as-resource (atom "original value"))
    "widget"            (the-widget-resource {:sprockets 3
                                              :reaction  "wow"})
    "die"               (yada/as-resource (fn []
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
