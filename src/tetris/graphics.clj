(ns tetris.graphics
  (:use [clojure.contrib.def :only (defvar)]
        tetris.data)
  (:import (java.awt Color Component Graphics Graphics2D)
           (javax.swing JComponent JPanel SwingUtilities)))

(defvar point-size 20
  "Size of a \"point\" on the screen in pixels. A \"point\" is the size of the
   building blocks in the tetris shapes, i.e. 1/m-size of the square.")

(defn point-to-screen-rect [pt]
  "Converts a point to a rectangle on the screen (more specifically, game panel).
   Returns '(x y width height). Does not draw anything on the screen."
  (map #(* point-size %)
       [(first pt) (second pt) 1 1]))

(defn fill-point [#^Graphics g pt #^Color color]
  "Displays point pt on graphic device g as a rectangle in color."
  (let [[x y width height] (point-to-screen-rect pt)]
    (.setColor g color)
    (.fillRect g x y width height)
    (.setColor g Color/black)
    (.drawRect g x y width height)))

(defn paint-block [g block]
  "Paints a block to g."
  (let [color (block-colors (:type block))
        shape (block-shape block)
        position (:position block)]
    (doseq [x (range m-size) y (range m-size)]
      (when (shape-element shape [x y])
        (fill-point g (map + position [x y]) color)))))

(defn paint-field [g]
  (doseq [x (range width) y (range height)]
    (let [type (get-element [x y])]
      (when-not (= type :empty)
        (fill-point g [x y] (block-colors type))))))

(defn change-key-listener [comp listener]
  "Removes all KeyListeners from comp and adds listener as the new KeyListener."
  (doseq [l (.getKeyListeners comp)]
    (.removeKeyListener comp l))
  (.addKeyListener comp listener))

(defn dimmer-panel [#^Color color & comps]
  "Returns a panel is generally transparent, but provides a 50% translucency
   with the specified colors over components comps."
  (let [panel (proxy [JPanel] []
                (paintComponent [#^Graphics g]
                  (proxy-super paintComponent g)
                  (doseq [#^JComponent comp comps]
                    (let [#^Graphics2D g2 g,
                          crect (SwingUtilities/convertRectangle
                                  comp (.getVisibleRect comp) this)]
                      (.fill g2 crect)))))]
    (doto panel
      ; If this is not OK for some reason, (.setColor g ...) works for sure.
      (.setForeground (Color. (.getRed color) (.getGreen color)
                              (.getBlue color) 180))
      (.setOpaque false))
    panel))

;;; Local Variables:
;;; indent-tabs-mode: nil
;;; End:
