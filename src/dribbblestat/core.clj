(ns dribbblestat.core
  (:require [clojure.data.json :as json]))

(def api-timeout 1000)
(def api-root "https://api.dribbble.com/v1/")
(declare api-key)

(defn- now
  [& args]
  (System/currentTimeMillis))

(defn- throttled
  [f timeout]
  (defonce last-call (atom 0))
  (let [interval (- (now) @last-call)]
    (when (< interval timeout)
      (Thread/sleep (- timeout interval)))
    (swap! last-call now)
    (f)))

(defn- add-auth
  [url]
  (str url "?access_token=" api-key))

(defn- make-url
  [endpoint]
  (add-auth (str api-root endpoint)))

(defn- user
  [id]
  (make-url (str "/users/" id)))

(defn- get-with-timeout
  [url timeout]
  (throttled #(slurp url) timeout))

(defn- get-data
  [url]
  (json/read-str (get-with-timeout url api-timeout)))

(defn- user-data
  [user-id]
  (get-data (user user-id)))

(defn- followers
  [user]
  ((comp get-data add-auth) (get user "followers_url")))

(defn- shots
  [follower]
  (let [url (get-in follower ["follower" "shots_url"])]
    (get-data (add-auth url))))

(defn- likers
  [shot]
  (let [url (get shot "likes_url")]
    (map #(get-in % ["user" "id"]) (get-data (add-auth url)))))

(defn- get-top-likers
  [user]
  (->>  user
        user-data
        followers
        (mapcat #(shots %))
        (mapcat #(likers %))
        frequencies
        (sort-by last)
        reverse
        (take 10)
        (map (fn[[user likes]] [(user-data user) likes]))))

(defn top-likers
  [{:keys [user api-key on-success on-error]}]
  (defonce api-key api-key)
  (future
    (try (on-success (get-top-likers user))
      (catch Exception e (on-error e)))))
