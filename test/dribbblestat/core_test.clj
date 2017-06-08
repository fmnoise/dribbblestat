(ns dribbblestat.core-test
  (:require [clojure.test :refer :all]
            [dribbblestat.core :refer :all]
            [clj-http.client :as client]
            [clj-http.fake :as fake]))

(defn- respond-with
  [body]
  (fn [_]
    {:status 200 :content-type "application/json" :body body}))

(defn- route
  [path]
  (re-pattern (str api-root path)))

(def result
    (fake/with-fake-routes
      { (route "/users(.*)")
        (respond-with
          (str "{ \"followers_url\": \"" api-root "/followers\" }"))

        (route "(.*)page=2(.*)")
        (respond-with "[]")

        (route "/followers(.*)")
        (respond-with
          (str "[{ \"follower\": { \"shots_url\": \"" api-root "/shots\" }}]"))

        (route "/shots(.*)")
        (respond-with
          (str "[{ \"likes_url\": \"" api-root "/likes\" }]"))

        (route "/likes(.*)")
        (respond-with
          (str "[{ \"user\": \"Fooman\" }, { \"user\": \"Barman\" }, { \"user\": \"Fooman\" }]"))}

      (top-likers :user 1
                  :api-key "fake"
                  :on-success identity
                  :on-error identity)))

(deftest top-likers-test
  (is (= @result '(["Fooman" 2] ["Barman" 1]))))
