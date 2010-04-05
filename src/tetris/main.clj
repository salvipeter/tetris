(ns tetris.main
  (:use [clojure.contrib.fcase :only (in-case)]
        [clojure.contrib.math :only (expt)]
        tetris.data
        tetris.graphics
        tetris.logic
        tetris.util)
  (:import (java.awt Color Dimension BorderLayout)
           (java.awt.event ActionListener KeyAdapter KeyEvent WindowAdapter
             WindowListener)
           (javax.swing JFrame JLabel JPanel Timer WindowConstants)))

(defn common-key-listener [gui]
  "Handles shortcuts that behave the same irrespective of whether the game is
   running or not."
  (proxy [KeyAdapter] []
    (keyPressed [e]
      (when (= (.getKeyCode e) KeyEvent/VK_Q)
        (.dispose (:frame gui))))))

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
(def pause-key-listener)
(def menu-key-listener)

(defn start-game! [gui]
  "Starts the game."
  (do
    (clear-field!)
    (dosync
      (ref-set level 1)
      (ref-set current-block (get-random-block))
      (ref-set next-block (get-random-block)))
    (clear-score! gui)
    (.repaint (:next gui))
    (change-key-listener (:panel gui) (game-key-listener gui))
    (.setDelay (:timer gui) (levels @level))
    (.start (:timer gui))
    (.repaint (:panel gui))))

(defn pause-game! [gui]
  "Pauses the game."
  (do (.stop (:timer gui))
      (change-key-listener (:panel gui) (pause-key-listener gui))))

(defn continue-game! [gui]
  "Continues the game after pause."
  (do (change-key-listener (:panel gui) (game-key-listener gui)))
      (.start (:timer gui)))

(defn end-game! [gui]
  "Ends the game."
  (do (.stop (:timer gui))
      (change-key-listener (:panel gui) (menu-key-listener gui))))

(defn quit-game! [gui]
  "Quits the game."
  (do (.stop (:timer gui))
      (.dispose (:frame gui))))

(defn menu-key-listener [gui]
  (proxy [KeyAdapter] []
    (keyPressed [e]
      (if (= (.getKeyCode e) KeyEvent/VK_Q)
        (quit-game! gui)
        (start-game! gui)))))

(defn pause-key-listener [gui]
  (proxy [KeyAdapter] []
    (keyPressed [e]
      (if (= (.getKeyCode e) KeyEvent/VK_Q)
        (quit-game! gui)
        (continue-game! gui)))))

(defn handle-collision [gui]
  "Handles the collision: deletes full rows or ends the game."
  (record-block! @current-block)
  (if (block-out-of-playfield? @current-block)
    (end-game! gui)
    (let [full (full-rows)]
      (when-not (empty? full)
        (doseq [y full] (expunge-row! y))
        (let [removed (count full)]
          (add-score! gui removed)))
      (dosync (ref-set current-block @next-block)
              (ref-set next-block (get-random-block)))
      (.repaint (:next gui)))))

(defn lower-block [gui]
  (if (no-collision? (fall @current-block))
    (dosync (alter current-block fall))
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
               (lower-block gui) 
               [KeyEvent/VK_SPACE]
               (do (dosync (alter current-block drop-down))
                   (handle-collision gui))
               [KeyEvent/VK_P]
               (pause-game! gui)
               [KeyEvent/VK_Q]
               (quit-game! gui))
      (.repaint (:panel gui)))))

(defn game []
  (let [timer (Timer. 0 nil)
        frame (JFrame. "Tetris")
        score-label (JLabel. "Score:    0 | Lines:   0 | Level: 1")
        next-panel (proxy [JPanel] []
                     (paintComponent [g]
                       (proxy-super paintComponent g)
                       (when @next-block
                         (paint-block g (assoc @next-block :position [0 0]))))
                     (getPreferredSize []
                       (Dimension. (* m-size point-size)
                                   (* m-size point-size))))
        panel (proxy [JPanel ActionListener] []
                (paintComponent [g]
                  (proxy-super paintComponent g)
                  (paint-field g)
                  (when @current-block
                    (paint-block g @current-block)))
                (actionPerformed [e]
                  (let [gui {:timer timer :frame frame :panel this
                             :next next-panel :score score-label}]
                    (lower-block gui))
                  (.repaint this))
                (getPreferredSize []
                  (Dimension. (* width point-size)
                              (* height point-size))))
        gui {:timer timer :frame frame :panel panel :next next-panel
             :score score-label}
        window-listener (proxy [WindowAdapter] []
                          (windowClosing [e]
                            (quit-game! gui)))]
    (dosync (ref-set current-block nil))
    (.setBackground next-panel Color/white)
    (.addActionListener timer panel)
    (doto panel
      (.setBackground Color/black)
      (.setFocusable true)
      (.addKeyListener (menu-key-listener gui)))
    (doto frame
      (.setLayout (BorderLayout.))
      (.add panel BorderLayout/CENTER)
      (.add next-panel BorderLayout/EAST)
      (.add score-label BorderLayout/SOUTH)
      (.setDefaultCloseOperation WindowConstants/DO_NOTHING_ON_CLOSE)
      (.addWindowListener window-listener)
      (.pack)
      (.setVisible true))))

;;; Local Variables:
;;; indent-tabs-mode: nil
;;; End:
