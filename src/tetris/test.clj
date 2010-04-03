;; Test "framework" :)
(ns tetris.test
  (:use [clojure.contrib.fcase :only (case)]
        [clojure.contrib.seq-utils :only (positions)]
        tetris.data
        tetris.logic
        tetris.graphics)
  (:import (java.awt Color Dimension)
           (java.awt.event KeyListener KeyEvent)
	   (javax.swing JPanel JFrame)))

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
     - space: next block type,
     - q: quit"
  (let [block (ref block)
	frame (JFrame. "Tetris")
	panel (proxy [JPanel KeyListener] []
		(paintComponent [g]
		  (proxy-super paintComponent g)
		  (paint-block g @block))
		(keyPressed [e]
		  (case (.getKeyCode e)
			KeyEvent/VK_RIGHT
			(dosync (alter block rotate-right))
			KeyEvent/VK_LEFT
			(dosync (alter block rotate-left))
			KeyEvent/VK_SPACE
			(dosync (alter block next-block))
			KeyEvent/VK_Q
			(.dispose frame))
		  (.repaint this))
		(getPreferredSize []
		  (Dimension. (* 4 point-size)
			      (* 4 point-size)))
		(keyReleased [e])
		(keyTyped [e]))]
    (doto panel (.setBackground Color/black)
	  (.setFocusable true) (.addKeyListener panel))
    (doto frame (.add panel) (.pack) (.setVisible true))))

(defn rotation-test []
  (rotation-test-panel (get-block :square 0)))

(defn movement-test-panel [start-block]
  "Displays a test panel, which enables the user to see whether the move
   functions work. Controls are:
     - o: rotate right,
     - u: rotate left,
     - l: move right,
     - j: move left,
     - i: next block type,
     - k: fall,
     - space: drop,
     - q: quit"
  (clear-field!)
  (let [block (ref start-block)
	new-block (fn []
		    (record-block! @block)
		    (dosync (ref-set block start-block)))
	frame (JFrame. "Tetris")
	panel (proxy [JPanel KeyListener] []
		(paintComponent [g]
		  (proxy-super paintComponent g)
		  (paint-field g)
		  (paint-block g @block))
		(keyPressed [e]
		  (case (.getKeyCode e)
			KeyEvent/VK_O
			(dosync (alter block rotate-right))
			KeyEvent/VK_U
			(dosync (alter block rotate-left))
			KeyEvent/VK_L
			(dosync (alter block move-right))
			KeyEvent/VK_J
			(dosync (alter block move-left))
			KeyEvent/VK_I
			(dosync (alter block next-block))
			KeyEvent/VK_K
			(do (dosync (alter block fall))
			    (when-not (no-collision? (fall @block))
			      (new-block)))
			KeyEvent/VK_SPACE
			(do (dosync (alter block drop-down))
			    (new-block))
			KeyEvent/VK_Q
			(.dispose frame))
		  (.repaint this))
		(getPreferredSize []
		  (Dimension. (* width point-size)
			      (* height point-size)))
		(keyReleased [e])
		(keyTyped [e]))]
    (doto panel (.setBackground Color/black)
	  (.setFocusable true) (.addKeyListener panel))
    (doto frame (.add panel) (.pack) (.setVisible true))))

(defn movement-test []
  (movement-test-panel (get-block :square 0 [(- (/ width 2) 2) 0])))
