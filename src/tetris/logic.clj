;; I don't know if this file is really should be separate from data. Anyway...
(ns tetris.logic
  ;(:require )
  (:use tetris.data)
  ;(:import )
  )

(defn fall
  "Moves a block downwards (does not check for collision).
Both normal falling and fast-falling should use this function."
  [block]
  (let [[x y] (:position block)]
    (assoc block :position [x (inc y)])))

(defn placeable? [block]
  "Judges whether the given block fits into the playing field."
  (let [[x0 y0] (:position block)
	shape (block-shape block)]
    (every? identity
	    (for [dx (range 4) dy (range 4)]
	      (let [x (+ x0 dx)
		    y (+ y0 dy)]
		(or (not (shape-element shape [dx dy]))
		    (and (<= 0 x (dec width))
			 (<= 0 y (dec height))
			 (= (get-element [x y]) :empty))))))))

(defn drop-down [block]
  "Does not update the field. Returns a fresh block."
  (last (take-while placeable? (iterate fall block))))

(defn full-rows
  "Returns a lazy list of y-coordinates."
  []
  (filter (fn [y]
	    (for [x (range width)]
	      (not-any? (= (get-element [x y]) :empty))))
	  (range height)))

(defn record-block! [block]
  (let [shape (block-shape block)
	[x0 y0] (:position block)]
    (doseq [x (range 4) y (range 4)]
      (when (shape-element shape [x y])
	(set-element! [(+ x0 x) (+ y0 y)] (:type block))))))

(defn expunge-row! [row]
  (doseq [y (range row) x (range width)]
    (set-element! [x (inc y)] (get-element [x y]))))

(defn rotate
  "Rotates a block to the left or right, depending on dir (+1 or -1, respectively)."
  [block dir]
  (let [n (count (block-types (:type block)))]
    (assoc block :rotation (mod (+ (:rotation block) dir) n))))
(defn rotate-maybe
  "Rotates a block if it fits."
  [block dir]
  (let [result (rotate block dir)]
    (or (and (placeable? result) result) block)))
(defn rotate-left [block] (rotate-maybe block 1))
(defn rotate-right [block] (rotate-maybe block -1))

(defn move
  "Moves a block to the left or right, depending on dir (+1 or -1, respectively)."
  [block dir]
  (let [[x y] (:position block)]
    (assoc block :position [(+ x dir) y])))
(defn move-maybe
  "Moves a block if it fits."
  [block dir]
  (let [result (move block dir)]
    (or (and (placeable? result) result) block)))
(defn move-left [block] (move-maybe block -1))
(defn move-right [block] (move-maybe block 1))
