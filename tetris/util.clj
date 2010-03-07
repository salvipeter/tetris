;; Utility ("library") functions.
(ns tetris.util
  ;(:require )
  ;(:use )
  ;(:import )
  )

(defn rpartial [f & fixed-args]
  (fn [& args] (apply f (concat args fixed-args))))