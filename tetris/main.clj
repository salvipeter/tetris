(ns tetris.main
  (:use [clojure.contrib.fcase :only (case)]
	[clojure.contrib.seq-utils :only (positions)]
        [tetris.graphics :only (gr-hello)])
  (:import (java.awt Color Dimension)
	   (java.awt.event KeyListener KeyEvent)
	   (javax.swing JPanel JFrame)))

;; Contrary to its name, this map contains not only the block types, but also
;; the rotation phases they can take."
(def block-types
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
		0 1 0 0]]})

;; Colors associated with the shape types."
(def block-colors
     {:square Color/red
      :lright Color/yellow
      :lleft  Color/green
      :sright Color/magenta
      :sleft  Color/gray
      :stick  Color/cyan
      :tshape Color/blue})

;; The block object."
(defstruct block :type :rotation :position)

(defn rotate
  "Rotates a block to the left or right, depending on dir (+1 or -1, respectively)."
  [block dir]
  (let [n (count (block-types (:type block)))]
    (assoc block :rotation (mod (+ (:rotation block) dir) n))))
(defn rotate-left [block] (rotate block 1))
(defn rotate-right [block] (rotate block -1))

;; TODO: put this to a separate "graphical layer/file".
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
	position [0 0]]
    (doall (for [x (range 4) y (range 4)]
	     (when-not (zero? (nth shape (+ (* 4 y) x)))
	       (fill-point g (map + position [x y]) color))))))

(defn next-block [block]
  "For testing: returns a block whose types comes after block's in block-types.
   The rotation of the new block will be 0."
  (let [types (keys block-types)
	pos (first (positions #(= % (:type block)) types))]
    (assoc block
      :type (nth types (mod (inc pos) (count types)))
      :rotation 0)))

(defn rotation-test-panel [block]
  "Displayes a test panel, which enables the user to see how the rotation phases
   of the block types look like. Controls are:
     - cursor right: rotate right,
     - cursor left: rotate left,
     - next block type: space."
  (let [block (ref block)
	panel (proxy [JPanel KeyListener] []
		(paintComponent [g]
				(proxy-super paintComponent g)
				(paint g @block))
		(keyPressed [e]
			    (case (.getKeyCode e)
			      KeyEvent/VK_RIGHT
			      (dosync (alter block rotate-right))
			      KeyEvent/VK_LEFT
			      (dosync (alter block rotate-left))
			      KeyEvent/VK_SPACE
			      (dosync (alter block next-block)))
			    (.repaint this))
		(getPreferredSize []
				  (Dimension. (* 4 point-size)
					      (* 4 point-size)))
		(keyReleased [e])
		(keyTyped [e]))
	frame (JFrame. "Tetris")]
    (doto panel (.setBackground Color/black) (.setFocusable true) (.addKeyListener panel))
    (doto frame (.add panel) (.pack) (.setVisible true))))
