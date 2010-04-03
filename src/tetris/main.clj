(ns tetris.main
  (:use [clojure.contrib.fcase :only (in-case)]
	[clojure.contrib.math :only (expt)]
	tetris.data
	tetris.graphics
	tetris.logic
	tetris.util)
  (:import (java.awt Color Dimension BorderLayout)
           (java.awt.event ActionListener KeyAdapter KeyEvent)
	   (javax.swing JFrame JLabel JPanel Timer)))

(defn get-random-block []
  (get-block (random-select (keys block-types))
	     0 [(- (/ width 2) 2) 0]))

(defn change-key-listener [comp listener]
  (doseq [l (.getKeyListeners comp)]
    (.removeKeyListener comp l))
  (.addKeyListener comp listener))

(defn update-score [gui]
  (.setText (:score gui)
	    (format "Score: %4d | Lines: %3d | Level: %2d"
		    @score @lines @level))
  (.repaint (:score gui)))

(defn clear-score! [gui]
  (dosync (ref-set score 0)
	  (ref-set lines 0))
  (update-score gui))

(defn add-score! [gui removed]
  (let [new-score (+ @score (* (expt 2 @level) ([0 1 3 5 8] removed)))]
    (dosync (ref-set score new-score)
	    (alter lines + removed)))
  (when (> @lines (* 20 @level))
    (dosync (alter level inc))
    (.setDelay (:timer gui) (levels @level)))
  (update-score gui))

(def game-key-listener)

(defn menu-key-listener [gui]
  (proxy [KeyAdapter] []
    (keyPressed [e]
      (if (= (.getKeyCode e) KeyEvent/VK_Q)
	(.dispose (:frame gui))
	(do 
	  (clear-field!)
	  (dosync (ref-set level 1)
		  (ref-set current-block (get-random-block)))
	  (clear-score! gui)
	  (change-key-listener (:panel gui) (game-key-listener gui))
	  (.setDelay (:timer gui) (levels @level))
	  (.start (:timer gui))
	  (.repaint (:panel gui)))))))

(defn reincarnate-block [gui]
  (record-block! @current-block)
  (let [full (full-rows)]
    (when-not (empty? full)
      (doseq [y full] (expunge-row! y))
      (let [removed (count full)]
	(add-score! gui removed))))
  (dosync (ref-set current-block (get-random-block)))
  (when-not (placeable? @current-block)
    (.stop (:timer gui))
    (change-key-listener (:panel gui) (menu-key-listener gui))))

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
	       (do (dosync (alter current-block drop-down))
		   (reincarnate-block gui))
	       [KeyEvent/VK_Q]
	       (do (.dispose (:frame gui))
		   (.stop (:timer gui))))
      (.repaint (:panel gui)))))

(defn game []
  (let [timer (Timer. 0 nil)
	frame (JFrame. "Tetris")
	score-label (JLabel. "Score:    0 | Lines:   0 | Level:  1")
	panel (proxy [JPanel ActionListener] []
		(paintComponent [g]
		  (proxy-super paintComponent g)
		  (paint-field g)
		  (when @current-block
		    (paint-block g @current-block)))
		(actionPerformed [e]
		  (let [gui {:timer timer :frame frame :panel this
			     :score score-label}]
		    (if (placeable? (fall @current-block))
		      (dosync (alter current-block fall))
		      (reincarnate-block gui)))
		  (.repaint this))
		(getPreferredSize []
		  (Dimension. (* width point-size)
			      (* height point-size))))
	gui {:timer timer :frame frame :panel panel :score score-label}]
    (dosync (ref-set current-block nil))
    (.addActionListener timer panel)
    (doto panel
      (.setBackground Color/black)
      (.setFocusable true)
      (.addKeyListener (menu-key-listener gui)))
    (doto frame (.setLayout (BorderLayout.)) (.add panel BorderLayout/CENTER)
	  (.add score-label BorderLayout/SOUTH) (.pack) (.setVisible true))))
