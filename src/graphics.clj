(ns tetris.graphics
  ;(:require )
  (:use tetris.data)
  ;(:import )
  )

;; Size of a "point" on the screen in pixels. A "point" is the size of the
;; building blocks in the tetris shapes, i.e. 1/4 of the square."
(def point-size 10)

(defn point-to-screen-rect [pt]
  "Converts a point to a rectangle on the screen (more specifically, game panel).
   Returns '(x y width height). Does not draw anything on the screen."
  (map #(* point-size %)
       [(first pt) (second pt) 1 1]))

(defn fill-point [g pt color]
  "Displays point pt on graphic device g as a rectangle in color."
  (let [[x y width height] (point-to-screen-rect pt)]
    (.setColor g color)
    (.fillRect g x y width height)))

(defn paint [g block]
  "Paints a block to g."
  (let [color (block-colors (:type block))
	shape (nth ((:type block) block-types) (:rotation block))
	position (:position block)]
    (doall (for [x (range 4) y (range 4)]
	     (when-not (zero? (nth shape (+ (* 4 y) x)))
	       (fill-point g (map + position [x y]) color))))))

