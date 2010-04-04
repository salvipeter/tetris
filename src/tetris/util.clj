;; Utility ("library") functions.
(ns tetris.util)

(defn rpartial [f & fixed-args]
  (fn [& args] (apply f (concat args fixed-args))))

(defn random-select [coll]
  (nth coll (rand-int (count coll))))

(defmacro for-every? [seq-exprs body-expr]
  `(every? identity (for ~seq-exprs ~body-expr)))

(defmacro for-some [seq-exprs body-expr]
  `(some identity (for ~seq-exprs ~body-expr)))

;;; Local Variables:
;;; indent-tabs-mode: nil
;;; End:
