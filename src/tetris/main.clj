(ns tetris.main
  (:use tetris.data
        tetris.test)
  ;(:import)
  )

(defn rotation-test []
  (rotation-test-panel (get-block :square 0)))

(defn movement-test []
  (movement-test-panel (get-block :square 0 [(- (/ width 2) 2) 0])))
