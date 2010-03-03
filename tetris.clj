(ns tetris
  (:use [clojure.contrib.fcase :only (case)]
	[clojure.contrib.seq-utils :only (positions)])
  (:import (java.awt Color Dimension)
	   (java.awt.event KeyListener KeyEvent)
	   (javax.swing JPanel JFrame)))

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

(def block-colors
     {:square Color/red
      :lright Color/yellow
      :lleft  Color/green
      :sright Color/magenta
      :sleft  Color/gray
      :stick  Color/cyan
      :tshape Color/blue})

(defstruct block :type :rotation :position)

(defn rotate
  "Rotates a block to the left or right, depending on dir (+1 or -1, respectively)."
  [block dir]
  (let [n (count (block-types (:type block)))]
    (assoc block :rotation (mod (+ (:rotation block) dir) n))))

(defn rotate-left [block] (rotate block 1))
(defn rotate-right [block] (rotate block -1))

(def point-size 10)
(defn point-to-screen-rect [pt]
  (map #(* point-size %)
       [(first pt) (second pt) 1 1]))
(defn fill-point [g pt color]
  (let [[x y width height] (point-to-screen-rect pt)]
    (.setColor g color)
    (.fillRect g x y width height)))

(defn paint [g block]
  (let [color (block-colors (:type block))
	shape (nth ((:type block) block-types) (:rotation block))
	position [0 0]]
    (doall (for [x (range 4) y (range 4)]
	     (when-not (zero? (nth shape (+ (* 4 y) x)))
	       (fill-point g (map + position [x y]) color))))))

(defn next-block [block]
  (let [types (keys block-types)
	pos (first (positions #(= % (:type block)) types))]
    (assoc block
      :type (nth types (mod (inc pos) (count types)))
      :rotation 0)))

(defn test-panel [block]
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
