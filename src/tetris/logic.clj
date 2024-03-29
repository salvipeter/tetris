(ns tetris.logic
  (:use tetris.data
        tetris.util))

(defn fall
  "Moves a block downwards (does not check for collision).
Both normal falling and fast-falling should use this function."
  [block]
  (let [[x y] (:position block)]
    (assoc block :position [x (inc y)])))

(defn no-collision? [block]
  "Judges whether the given block fits into the playing field. Pixels
above the upper bounds of the playing field are not checked."
  (let [[x0 y0] (:position block)
        shape (block-shape block)]
    (for-every? [dx (range m-size) dy (range m-size)]
      (let [x (+ x0 dx)
            y (+ y0 dy)]
        (or (not (shape-element shape [dx dy]))
            (and (<= 0 x (dec width))
                 (or (< y 0)
                     (and (<= y (dec height))
                          (= (get-element [x y]) :empty)))))))))

(defn block-where? [block]
  "Tells the positions of all rows in the block's shape matrix, relative to the
playing field. The possible values are :in, :out and nil; the latter is
returned for rows not containing a block pixel. Only the y coordinate is
considered."
  (let [[_ y0] (:position block)
        shape (block-shape block)]
    (for [dy (range m-size)]
      (when (for-some [dx (range m-size)] (shape-element shape [dx dy]))
        (if (<= 0 (+ y0 dy) (dec height)) :in :out)))))

(defn block-in-playfield? [block]
  "Checks if block is in the play field; that is, at least one of its
pixels is. Assumes that the block is inside the field horizontally."
  (some #{:in} (block-where? block)))

(defn block-out-of-playfield? [block]
  "Checks if block is out of the play field; that is, at least one of
its pixels is."
  (some #{:out} (block-where? block)))

(defn block-start-offset [block]
  "Returns the y offset block needs so that it is just above the playing
field (sans empty lines)."
  (for-some [offset (range (- m-size) 0)
             row (reverse (block-where? block))]
    (and row offset)))

(defn get-random-block []
  "Returns a random block positioned just above the playing field."
  (let [type (random-select (keys block-types))
        rotation (rand-int (count (block-types type)))
        block (get-block type rotation)]
    (assoc block :position [(- (/ width 2) 2) (block-start-offset block)])))

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
    (doseq [dx (range m-size) dy (range m-size)]
      (let [x (+ x0 dx)
            y (+ y0 dy)]
        (when (and (>= y 0) (shape-element shape [dx dy]))
          (set-element! [x y] (:type block)))))))

(defn expunge-row! [row]
  "Deletes a (full) row."
  (doseq [y (range row 0 -1) x (range width)]
    (set-element! [x y] (get-element [x (dec y)]))))

(defn rotate
  "Rotates a block to the left or right, depending on dir (+1 or -1,
respectively)."
  [block dir]
  (let [n (count (block-types (:type block)))]
    (assoc block :rotation (mod (+ (:rotation block) dir) n))))
(defn rotate-maybe
  "Rotates a block if it is in the playing field and it is not in
collision course. :)"
  [block dir]
  (let [result (rotate block dir)]
    (or (and (block-in-playfield? result) (no-collision? result) result)
        block)))
(defn rotate-left [block] (rotate-maybe block 1))
(defn rotate-right [block] (rotate-maybe block -1))

(defn move
  "Moves a block to the left or right, depending on dir (+1 or -1,
respectively)."
  [block dir]
  (let [[x y] (:position block)]
    (assoc block :position [(+ x dir) y])))
(defn move-maybe
  "Moves a block if it is in the playing field and it is not in
collision course. :)"
  [block dir]
  (let [result (move block dir)]
    (or (and (block-in-playfield? result) (no-collision? result) result)
        block)))
(defn move-left [block] (move-maybe block -1))
(defn move-right [block] (move-maybe block 1))

;;; Local Variables:
;;; indent-tabs-mode: nil
;;; End:
