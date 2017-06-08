(ns dribbblestat.core
  (:require [clj-http.client :as client]))

(def api-timeout 1000)
(def api-root "https://api.dribbble.com/v1")
(def per-page 100)
(def ^:dynamic api-key nil)
(def last-call (atom 0))
(def connection-timeout 5000)

(defn- now
  [& args]
  (System/currentTimeMillis))

(defn- throttled
  [f timeout]
  (let [interval (- (now) @last-call)]
    (when (< interval timeout)
      (Thread/sleep (- timeout interval)))
    (let [result (f)]
      (swap! last-call now)
      result)))

(defn- make-url
  [endpoint]
  (str api-root endpoint))

(defn- user-url
  [id]
  (make-url (str "/users/" id)))

(defn- fetch-with-timeout
  [url page timeout]
  (throttled
    #(client/get url
      {:oauth-token api-key
       :query-params {"page" page "per_page" per-page}
       :connection-timeout connection-timeout
       :as :json})
    timeout))

(defn- fetch-data
  ([url]
   (fetch-data url 1))
  ([url page]
   (:body (fetch-with-timeout url page api-timeout))))

(defn- fetch-collection
  [url]
  (loop [page 1
         collection []]
    (let [page-contents (fetch-data url page)]
      (if (empty? page-contents)
        collection
        (recur (inc page) (into collection page-contents))))))

(defn- fetch-user
  [user-id]
  (fetch-data (user-url user-id)))

(defn- fetch-followers
  [user]
  (fetch-collection (get user :followers_url)))

(defn- fetch-shots
  [follower]
  (let [url (get-in follower [:follower :shots_url])]
    (fetch-collection url)))

(defn- fetch-likers
  [shot]
  (let [url (get shot :likes_url)]
    (map #(get % :user) (fetch-collection url))))

(defn- fetch-top-likers
  [user]
  (->>  user
        fetch-user
        fetch-followers
        (mapcat fetch-shots)
        (mapcat fetch-likers)
        frequencies
        (sort-by last >)
        (take 10)))

(defn top-likers
  [& {:keys [user api-key on-success on-error]}]
  (future
    (with-bindings {#'api-key api-key}
      (try (on-success (fetch-top-likers user))
          (catch Exception e (on-error e))))))
