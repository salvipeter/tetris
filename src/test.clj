;; Test "framework" :)
(ns tetris.test
  ;(:require )
  (:use [clojure.contrib.fcase :only (case)]
        [clojure.contrib.seq-utils :only (positions)]
        tetris.data
        tetris.logic
        tetris.graphics)
  (:import (java.awt Color Dimension)
           (java.awt.event KeyListener KeyEvent)
	   (javax.swing JPanel JFrame))
  )

(defn next-block [block]
  "For testing: returns a block whose types comes after block's in block-types.
   The rotation of the new block will be 0."
  (let [types (keys block-types)
	pos (first (positions #(= % (:type block)) types))]
    (assoc block
      :type (nth types (mod (inc pos) (count types)))
      :rotation 0)))

(defn rotation-test-panel [block]
  "Displays a test panel, which enables the user to see how the rotation phases
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

