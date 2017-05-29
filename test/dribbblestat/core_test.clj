(ns dribbblestat.core-test
  (:require [clojure.test :refer :all]
            [dribbblestat.core :refer :all]
            [clojure.data.json :as json]
            [clj-http.client :as client]
            [clj-http.fake :as fake]))

(defn- respond-with
  [body]
  (fn [_]
    {:status 200 :content-type "application/json" :body body}))

(def result
    (fake/with-fake-routes
      { (re-pattern (str api-root "/users(.*)"))
        (respond-with
          (str "{ \"followers_url\": \"" api-root "/followers\" }"))

        (re-pattern (str api-root "(.*)page=2(.*)"))
        (respond-with "[]")

        (re-pattern (str api-root "/followers(.*)"))
        (respond-with
          (str "[{ \"follower\": { \"shots_url\": \"" api-root "/shots\" }}]"))

        (re-pattern (str api-root "/shots(.*)"))
        (respond-with
          (str "[{ \"likes_url\": \"" api-root "/likes\" }]"))

        (re-pattern (str api-root "/likes(.*)"))
        (respond-with
          (str "[{ \"user\": \"Fooman\" } { \"user\": \"Barman\" } { \"user\": \"Fooman\" }]"))}

      (top-likers :user 1
                  :api-key "fake"
                  :on-success identity
                  :on-error identity)))

(deftest top-likers-test
  (is (= @result '(["Fooman" 2] ["Barman" 1]))))
