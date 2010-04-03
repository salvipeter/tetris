;; I don't know if this file is really should be separate from data. Anyway...
(ns tetris.logic
  (:use tetris.data))

(defn fall
  "Moves a block downwards (does not check for collision).
Both normal falling and fast-falling should use this function."
  [block]
  (let [[x y] (:position block)]
    (assoc block :position [x (inc y)])))

(defn no-collision? [block]
  "Judges whether the given block fits into the playing field. Pixels above the
   upper bounds of the playing field are not checked."
  (let [[x0 y0] (:position block)
	shape (block-shape block)]
    ;; Same as (apply for ...)?
    (every? identity
	    (for [dx (range 4) dy (range 4)]
	      (let [x (+ x0 dx)
		    y (+ y0 dy)]
                (or (not (shape-element shape [dx dy]))
                    (and (<= 0 x (dec width))
                         (or (< y 0)
                             (and (<= y (dec height))
                                  (= (get-element [x y]) :empty))))))))))

(defn collision? [block]
  "Judges if block collides with something - another block or the edges of the
   playing field."
  (do
    (println "Collision: " (no-collision? block) " for block " block)
    (not (no-collision? block))))

(defn block-in-playfield? [block]
  "Checks if block is in the play field; that is, at least one of its pixels is."
  (let [[x0 y0] (:position block)
	shape (block-shape block)]
    (some identity
          (for [dx (range 4) dy (range 4)]
            (let [x (+ x0 dx)
                  y (+ y0 dy)]
              (and
                (>= y 0)
                (shape-element shape [dx dy])))))))

(defn block-out-of-playfield? [block]
  "Checks if block is out of the play field; that is, at least one of its pixels is."
  (let [[x0 y0] (:position block)
	shape (block-shape block)]
    (not (every? identity
          (for [dx (range 4) dy (range 4)]
            (let [x (+ x0 dx)
                  y (+ y0 dy)]
              (or (not (shape-element shape [dx dy]))
                  (and (<= 0 x (dec width))
                       (<= 0 y (dec height))))))))))

(defn drop-down [block]
  "Does not update the field. Returns a fresh block."
  (last (take-while no-collision? (iterate fall block))))

(defn full-rows
  "Returns a lazy list of y-coordinates."
  []
  (filter (fn [y] (not-any? #(= (get-element [% y]) :empty) (range width)))
	  (range height)))

(defn record-block! [block]
  "Just another brick in the wall."
  (let [shape (block-shape block)
	[x0 y0] (:position block)]
    (doseq [dx (range 4) dy (range 4)]
      (let [x (+ x0 dx)
            y (+ y0 dy)]
        (when (and (>= y 0) (shape-element shape [dx dy]))
          (set-element! [x y] (:type block)))))))

(defn expunge-row! [row]
  "Deletes a (full) row."
  (doseq [y (range row 0 -1) x (range width)]
    (set-element! [x y] (get-element [x (dec y)]))))

(defn rotate
  "Rotates a block to the left or right, depending on dir (+1 or -1, respectively)."
  [block dir]
  (let [n (count (block-types (:type block)))]
    (assoc block :rotation (mod (+ (:rotation block) dir) n))))
(defn rotate-maybe
  "Rotates a block if it is in the playing field and it is not in collision course. :)"
  [block dir]
  (let [result (rotate block dir)]
    (or (and (block-in-playfield? result) (no-collision? result) result) block)))
(defn rotate-left [block] (rotate-maybe block 1))
(defn rotate-right [block] (rotate-maybe block -1))

(defn move
  "Moves a block to the left or right, depending on dir (+1 or -1, respectively)."
  [block dir]
  (let [[x y] (:position block)]
    (assoc block :position [(+ x dir) y])))
(defn move-maybe
  "Moves a block if it is in the playing field and it is not in collision course. :)"
  [block dir]
  (let [result (move block dir)]
    (or (and (block-in-playfield? result) (no-collision? result) result) block)))
(defn move-left [block] (move-maybe block -1))
(defn move-right [block] (move-maybe block 1))
