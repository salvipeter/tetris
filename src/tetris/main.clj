(ns tetris.main
  (:use [clojure.contrib.fcase :only (in-case)]
        [clojure.contrib.math :only (expt)]
        [clojure.contrib.swing-utils :only (make-menubar)]
        tetris.data
        tetris.graphics
        tetris.logic
        tetris.util)
  (:import (java.awt Color Dimension BorderLayout)
           (java.awt.event ActionListener KeyAdapter KeyEvent WindowAdapter
                           WindowListener)
           (javax.swing BorderFactory JFrame JLabel JPanel Timer
                        WindowConstants)))

(defn actions-with-key-event-keys [actions]
  (concat (mapcat (fn [[keys action]]
                    (list (into [] (map (fn [key] `(. KeyEvent ~key)) keys))
                          action))
                  (partition 2 actions))
          (when (odd? (count actions)) (list (last actions)))))

(defmacro def-key-listener [name [gui-var] actions & finally]
  (let [key (gensym "key")]
    `(defn ~name [~gui-var]
       (proxy [KeyAdapter] []
	 (keyPressed [~(with-meta key {:tag KeyEvent})]
	   (in-case (.getKeyCode ~key)
	     ~@(actions-with-key-event-keys
                 (concat `([VK_Q] (quit-game! ~gui-var)) actions)))
	   ~@finally)))))

(defn change-key-listener [comp listener]
  "Removes all KeyListeners from comp and adds listener as the new KeyListener."
  (doseq [l (.getKeyListeners comp)]
    (.removeKeyListener comp l))
  (.addKeyListener comp listener))

(defn score-string []
  (format "<HTML><BODY>Score: %4d<BR>Lines: %3d<BR>Level: %2d</BODY></HTML>"
          @score @lines @level))

(defn update-score [gui]
  (.setText (:score gui) (score-string))
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
  (clear-field!)
  (dosync (ref-set level 1)
          (ref-set current-block (get-random-block))
          (ref-set next-block (get-random-block)))
  (clear-score! gui)
  (.repaint (:next gui))
  (change-key-listener (:panel gui) (game-key-listener gui))
  (.setVisible (.getGlassPane (:frame gui)) false)
  (.setDelay (:timer gui) (levels @level))
  (.start (:timer gui))
  (.repaint (:panel gui)))

(defn pause-game! [gui]
  "Pauses the game."
  (.stop (:timer gui))
  (.setVisible (.getGlassPane (:frame gui)) true)
  (change-key-listener (:panel gui) (pause-key-listener gui)))

(defn continue-game! [gui]
  "Continues the game after pause."
  (.setVisible (.getGlassPane (:frame gui)) false)
  (change-key-listener (:panel gui) (game-key-listener gui))
  (.start (:timer gui)))

(defn end-game! [gui]
  "Ends the game."
  (.stop (:timer gui))
  (.setVisible (.getGlassPane (:frame gui)) true)
  (change-key-listener (:panel gui) (menu-key-listener gui)))

(defn quit-game! [gui]
  "Quits the game."
  (.stop (:timer gui))
  (.dispose (:frame gui)))

(def-key-listener menu-key-listener [gui]
  [;; only else-branch here (i.e. anything except VK_Q)
   (start-game! gui)])

(def-key-listener pause-key-listener [gui]
  [;; only else-branch here (i.e. anything except VK_Q)
   (continue-game! gui)])

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

(def-key-listener game-key-listener [gui]
  [[VK_O]          (dosync (alter current-block rotate-right))
   [VK_U VK_UP]    (dosync (alter current-block rotate-left))
   [VK_L VK_RIGHT] (dosync (alter current-block move-right))
   [VK_J VK_LEFT]  (dosync (alter current-block move-left))
   [VK_K VK_DOWN]  (lower-block gui)
   [VK_SPACE]      (do (dosync (alter current-block drop-down))
		       (handle-collision gui))
   [VK_P]          (pause-game! gui)]
  (.repaint (:panel gui)))

(defn handler-test [event]
  (println "Handling test event."))

(defn create-menus []
  ;; use :accelerator for shortcuts and :handler for action handler functions
  ;; also valid keys are :command-key :long-desc :short-desc :icon
  (make-menubar
   [{:name "Game" :mnemonic KeyEvent/VK_G
     :items [{:name "Start" :mnemonic KeyEvent/VK_S :handler handler-test}
             {:name "Highscores" :mnemonic KeyEvent/VK_H :handler handler-test}
             {}                         ; separator
             {:name "Quit" :mnemonic KeyEvent/VK_Q :handler handler-test}]}
    {:name "Options" :mnemonic KeyEvent/VK_O
     :items [{:name "Configure keys..." :mnemonic KeyEvent/VK_K
              :handler handler-test}
             {:name "Preferences..." :mnemonic KeyEvent/VK_P
              :handler handler-test}]}
    {:name "Help" :mnemonic KeyEvent/VK_H
     :items [{:name "About..." :mnemonic KeyEvent/VK_A
              :handler handler-test}]}]))

(defn game []
  (let [timer (Timer. 0 nil)
        frame (JFrame. "Tetris")
        score-label (JLabel. (score-string))
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
    (let [left-panel (JPanel. (BorderLayout.))
          right-panel (JPanel. (BorderLayout.))]
      (doto left-panel
        (.add panel BorderLayout/CENTER)
        (.setBorder (BorderFactory/createEmptyBorder 5 5 5 5)))
      (doto right-panel
        (.add next-panel BorderLayout/NORTH)
        (.add score-label BorderLayout/CENTER)
        (.setBorder (BorderFactory/createEmptyBorder 5 5 5 5)))
      (doto frame
        (.setLayout (BorderLayout.))
        (.setJMenuBar (create-menus))
        (.add left-panel BorderLayout/CENTER)
        (.add right-panel BorderLayout/EAST)
        (.setDefaultCloseOperation WindowConstants/DO_NOTHING_ON_CLOSE)
        (.addWindowListener window-listener)
        (.setGlassPane (dimmer-panel Color/DARK_GRAY panel next-panel))
        (.pack)
        (.setVisible true)))
    (.setVisible (.getGlassPane frame) true)))

;;; Local Variables:
;;; indent-tabs-mode: nil
;;; End:
