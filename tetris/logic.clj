;; I don't know if this file is really should be separate from data. Anyway...
(ns tetris.logic
  ;(:require )
  (:use tetris.data)
  ;(:import )
  )

(defn rotate
  "Rotates a block to the left or right, depending on dir (+1 or -1, respectively)."
  [block dir]
  (let [n (count (block-types (:type block)))]
    (assoc block :rotation (mod (+ (:rotation block) dir) n))))
(defn rotate-left [block] (rotate block 1))
(defn rotate-right [block] (rotate block -1))