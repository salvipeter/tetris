(ns tetris.main
  (:use [clojure.contrib.fcase :only (in-case)]
	tetris.data
	tetris.graphics
	tetris.logic
        tetris.test
	tetris.util)
  (:import (java.awt Color Dimension)
           (java.awt.event ActionListener KeyAdapter KeyEvent)
	   (javax.swing JFrame JPanel Timer)))

(defn rotation-test []
  (rotation-test-panel (get-block :square 0)))

(defn movement-test []
  (movement-test-panel (get-block :square 0 [(- (/ width 2) 2) 0])))

(defn get-random-block []
  (get-block (random-select (keys block-types))
	     0 [(- (/ width 2) 2) 0]))

(defn change-key-listener [comp listener]
  (doseq [l (.getKeyListeners comp)]
    (.removeKeyListener comp l))
  (.addKeyListener comp listener))

(def game-key-listener)

(defn menu-key-listener [gui]
  (proxy [KeyAdapter] []
    (keyPressed [e]
      (if (= (.getKeyCode e) KeyEvent/VK_Q)
	(.dispose (:frame gui))
	(do 
	  (clear-field!)
	  (change-key-listener (:panel gui) (game-key-listener gui))
	  (dosync (ref-set current-block (get-random-block)))
	  (.start (:timer gui))
	  (.repaint (:panel gui)))))))

(defn reincarnate-block [gui]
  (record-block! @current-block)
  (let [full (full-rows)]
    (when-not (empty? full)
      (doseq [y full] (expunge-row! y))))
  (dosync (ref-set current-block (get-random-block)))
  (when-not (placeable? @current-block)
    (.stop (:timer gui))
    (change-key-listener (:panel gui) (menu-key-listener gui))))

(defn reincarnate-block-if-needed [gui]
  (when-not (placeable? (fall @current-block))
    (reincarnate-block gui)))

(defn game-key-listener [gui]
  (proxy [KeyAdapter] []
    (keyPressed [e]
      (in-case (.getKeyCode e)
	       [KeyEvent/VK_O]
	       (dosync (alter current-block rotate-right))
	       [KeyEvent/VK_U KeyEvent/VK_UP]
	       (dosync (alter current-block rotate-left))
	       [KeyEvent/VK_L KeyEvent/VK_RIGHT]
	       (dosync (alter current-block move-right))
	       [KeyEvent/VK_J KeyEvent/VK_LEFT]
	       (dosync (alter current-block move-left))
	       [KeyEvent/VK_K KeyEvent/VK_DOWN]
	       (dosync (alter current-block fall))
	       [KeyEvent/VK_SPACE]
	       (dosync (alter current-block drop-down))
	       [KeyEvent/VK_Q]
	       (do (.dispose (:frame gui))
		   (.stop (:timer gui))))
      (reincarnate-block-if-needed gui)
      (.repaint (:panel gui)))))

(defn game []
  (let [timer (Timer. @turn-millis nil)
	frame (JFrame. "Tetris")
	panel (proxy [JPanel ActionListener] []
		(paintComponent [g]
		  (proxy-super paintComponent g)
		  (paint-field g)
		  (when @current-block
		    (paint-block g @current-block)))
		(actionPerformed [e]
		  (let [gui {:timer timer :frame frame :panel this}]
		    (dosync (alter current-block fall))
		    (reincarnate-block-if-needed gui))
		  (.repaint this))
		(getPreferredSize []
		  (Dimension. (* width point-size)
			      (* height point-size))))
	gui {:timer timer :frame frame :panel panel}]
    (dosync (ref-set current-block nil))
    (.addActionListener timer panel)
    (doto panel
      (.setBackground Color/black)
      (.setFocusable true)
      (.addKeyListener (menu-key-listener gui)))
    (doto frame (.add panel) (.pack) (.setVisible true))))
