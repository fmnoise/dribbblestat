(ns dribbblestat.core
  (:require [clojure.data.json :as json]))

(def api-timeout 1000)

(defmacro promise-with-callback
  [callback]
  `(let [promise-with-callback# (promise)]
    (future
      (let [promise-result# @promise-with-callback#]
        (~callback promise-result#)))
    promise-with-callback#))

(defn- add-auth
  [url]
  (str url "?access_token=" api-key))

(defn- make-url
  [endpoint]
  (add-auth (str "https://api.dribbble.com/v1/" endpoint)))

(defn- user
  [id]
  (make-url (str "/users/" id)))

(defn- get-with-timeout
  [url timeout]
  (let [data (slurp url)]
    (Thread/sleep timeout)
    data))

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
  [{:keys [user api-key]} callback]
  (defonce api-key api-key)
  (let [promise (promise-with-callback callback)]
    (future (deliver promise (get-top-likers user)))))
