(ns dribbblestat.core
  (:require [clojure.data.json :as json]
            [clj-http.client :as client]))

(def api-timeout 1000)
(def api-root "https://api.dribbble.com/v1")
(def per-page 100)
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
    (let [result (f)]
      (swap! last-call now)
      result)))

(defn- make-url
  [endpoint]
  (str api-root endpoint))

(defn- user
  [id]
  (make-url (str "/users/" id)))

(defn- get-with-timeout
  [url page timeout]
  (throttled
    #(client/get url {:oauth-token api-key :query-params {"page" page "per_page" per-page}})
    timeout))

(defn- get-data
  ([url]
   (get-data url 1))
  ([url page]
   (json/read-str (:body (get-with-timeout url page api-timeout)))))

(defn- get-collection
  [url]
  (loop [page 1
         collection []]
    (let [page-contents (get-data url page)]
      (if (empty? page-contents)
        collection
        (recur (inc page) (into collection page-contents))))))

(defn- user-data
  [user-id]
  (get-data (user user-id)))

(defn- followers
  [user]
  (get-collection (get user "followers_url")))

(defn- shots
  [follower]
  (let [url (get-in follower ["follower" "shots_url"])]
    (get-collection url)))

(defn- likers
  [shot]
  (let [url (get shot "likes_url")]
    (map #(get % "user") (get-collection url))))

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
        (take 10)))

(defn top-likers
  [& {:keys [user api-key on-success on-error]}]
  (def api-key api-key)
  (future
    (try (on-success (get-top-likers user))
      (catch Exception e (on-error e)))))
