;; Defines the (global) data types, etc.
(ns tetris.data
  (:use [clojure.contrib.def :only (defvar)])
  (:import (java.awt Color)))

(defvar block-types
  {:square [[0 0 0 0
             0 1 1 0
             0 1 1 0
             0 0 0 0]]
   :lright [[0 1 0 0
             0 1 0 0
             0 1 1 0
             0 0 0 0]
            [0 0 1 0
             1 1 1 0
             0 0 0 0
             0 0 0 0]
            [1 1 0 0
             0 1 0 0
             0 1 0 0
             0 0 0 0]
            [0 0 0 0
             1 1 1 0
             1 0 0 0
             0 0 0 0]]
   :lleft [[0 0 1 0
            0 0 1 0
            0 1 1 0
            0 0 0 0]
           [0 0 0 0
            0 1 1 1
            0 0 0 1
            0 0 0 0]
           [0 0 1 1
            0 0 1 0
            0 0 1 0
            0 0 0 0]
           [0 1 0 0
            0 1 1 1
            0 0 0 0
            0 0 0 0]]
   :sright [[0 0 0 0
             0 1 1 0
             1 1 0 0
             0 0 0 0]
            [0 1 0 0
             0 1 1 0
             0 0 1 0
             0 0 0 0]]
   :sleft [[0 0 0 0
            0 1 1 0
            0 0 1 1
            0 0 0 0]
           [0 0 1 0
            0 1 1 0
            0 1 0 0
            0 0 0 0]]
   :stick [[0 1 0 0
            0 1 0 0
            0 1 0 0
            0 1 0 0]
           [0 0 0 0
            1 1 1 1
            0 0 0 0
            0 0 0 0]]
   :tshape [[0 0 0 0
             0 1 0 0
             1 1 1 0
             0 0 0 0]
            [0 0 0 0
             0 1 0 0
             1 1 0 0
             0 1 0 0]
            [0 0 0 0
             0 0 0 0
             1 1 1 0
             0 1 0 0]
            [0 0 0 0
             0 1 0 0
             0 1 1 0
             0 1 0 0]]}
  "Contrary to its name, this map contains not only the block types, but also
the rotation phases they can take.")

(defvar m-size 4
  "Row & column size of the block matrices.")

(defvar block-colors
  {:square Color/red
   :lright Color/yellow
   :lleft  Color/green
   :sright Color/magenta
   :sleft  Color/gray
   :stick  Color/cyan
   :tshape Color/blue}
  "Colors associated with the shape types.")

;; The block object.
(defstruct block :type :rotation :position)

(let [default-block (struct block :square 0 [0 0])]
  (defn get-block
    "Creates a block."
    ([] default-block)
    ([type] (assoc default-block :type type))
    ([type rotation] (assoc default-block :type type, :rotation rotation))
    ([type rotation position] (struct block type rotation position))))

(defn block-shape [block]
  "Returns block's shape matrix."
  (((:type block) block-types) (:rotation block)))
(defn shape-element [shape [x y]]
  "Tells whether [x y] in shape's bit matrix belongs to the shape, or is empty."
  (not (zero? (shape (+ (* m-size y) x)))))

(def width 14)
(def height 20)
(def field (take (* width height) (map ref (repeat :empty))))
(def current-block (ref nil))
(def next-block (ref nil))

(defn clear-field! []
  (doseq [element field]
    (dosync (ref-set element :empty))))

(defn get-element
  "One `pixel' of the playing field."
  [[x y]]
  @(nth field (+ (* y width) x)))

(defn set-element!
  "Set one `pixel' of the playing field."
  [[x y] val]
  (dosync (ref-set (nth field (+ (* y width) x)) val)))

(def levels [nil 1000 800 600 500 400 300 240 180 130 80])
(def level (ref 1))
(def lines (ref 0))
(def score (ref 0))

;;; Local Variables:
;;; indent-tabs-mode: nil
;;; End:
