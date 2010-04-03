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
	     0 [(- (/ width 2) 2) -4]))

(defn change-key-listener [comp listener]
  (println "Changing listener...")
  (doseq [l (.getKeyListeners comp)]
    (.removeKeyListener comp l))
  (.addKeyListener comp listener))

(defn set-score! [gui new-score]
  (dosync (ref-set score new-score))
  (.setText (:score gui) (format "Score: %d" @score))
  (.repaint (:score gui)))

(def game-key-listener)

(defn menu-key-listener [gui]
  (proxy [KeyAdapter] []
    (keyPressed [e]
      (if (= (.getKeyCode e) KeyEvent/VK_Q)
	(.dispose (:frame gui))
	(do 
	  (clear-field!)
	  (set-score! gui 0)
	  (dosync (ref-set level 5) (ref-set current-block (get-random-block)))
	  (change-key-listener (:panel gui) (game-key-listener gui))
	  (.setDelay (:timer gui) (levels @level))
          (println "START!")
	  (.start (:timer gui))
	  (.repaint (:panel gui)))))))

(defn handle-collision [gui]
  "Handles the collision: deletes full rows or ends the game."
  (println "Handling!")
  (record-block! @current-block)
  (if (block-out-of-playfield? @current-block)
      (do
        (.stop (:timer gui))
        (change-key-listener (:panel gui) (menu-key-listener gui)))
      (do
        (let [full (full-rows)]
          (when-not (empty? full)
            (doseq [y full] (expunge-row! y))
            (let [lines (count full)]
              (set-score! gui (+ @score (* (expt 2 @level) ([0 1 3 5 8] lines)))))))
        (dosync (ref-set current-block (get-random-block))))))

(defn something-happened [gui]
  "Called when something happens: user input, timer fired, etc. to handle the
   possible consequences."
  (when (collision? (fall @current-block))
    (handle-collision gui)))

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
      (something-happened gui)
      (.repaint (:panel gui)))))

(defn game []
  (let [timer (Timer. 0 nil)
	frame (JFrame. "Tetris")
	score-label (JLabel. "Score: 0")
	panel (proxy [JPanel ActionListener] []
		(paintComponent [g]
		  (proxy-super paintComponent g)
		  (paint-field g)
		  (when @current-block
		    (paint-block g @current-block)))
		(actionPerformed [e]
                  (println "ACTION!")
		  (let [gui {:timer timer :frame frame :panel this
			     :score score-label}]
		    (dosync (alter current-block fall))
		    (something-happened gui))
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
