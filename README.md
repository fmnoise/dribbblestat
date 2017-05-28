# dribbblestat

A Clojure library for getting top 10 likers for given user followers' shots from dribbble

## Usage

```clojure
(require '[dribbblestat.core :as d])

(d/top-likers :user 1
              :api-key "dribbble-api-key"
              :on-success println
              :on-error println})

;; [[{,,,user data map} ,,,likes count] ,,, ]
```

## License

Copyright © 2017 fmnoise

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
