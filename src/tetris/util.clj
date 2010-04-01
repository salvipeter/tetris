;; Utility ("library") functions.
(ns tetris.util)

(defn rpartial [f & fixed-args]
  (fn [& args] (apply f (concat args fixed-args))))

(defn random-select [coll]
  (nth coll (rand (count coll))))
