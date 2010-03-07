;; Defines the (global) data types, etc.
(ns tetris.data
  ;(:require )
  (:use [clojure.contrib.def :only (defvar)])
  (:import (java.awt Color))
  )

(defvar block-types
     {:square [[0 0 0 0
		0 1 1 0
		0 1 1 0
		0 0 0 0]]
      :lright [[0 1 0 0
		0 1 0 0
		0 1 1 0
		0 0 0 0]
	       [0 0 1 0
		1 1 1 0
		0 0 0 0
		0 0 0 0]
	       [1 1 0 0
		0 1 0 0
		0 1 0 0
		0 0 0 0]
	       [0 0 0 0
		1 1 1 0
		1 0 0 0
		0 0 0 0]]
      :lleft [[0 0 1 0
	       0 0 1 0
	       0 1 1 0
	       0 0 0 0]
	      [0 0 0 0
	       0 1 1 1
	       0 0 0 1
	       0 0 0 0]
	      [0 0 1 1
	       0 0 1 0
	       0 0 1 0
	       0 0 0 0]
	      [0 1 0 0
	       0 1 1 1
	       0 0 0 0
	       0 0 0 0]]
      :sright [[0 0 0 0
		0 1 1 0
		1 1 0 0
		0 0 0 0]
	       [0 1 0 0
		0 1 1 0
		0 0 1 0
		0 0 0 0]]
      :sleft [[0 0 0 0
	       0 1 1 0
	       0 0 1 1
	       0 0 0 0]
	      [0 0 1 0
	       0 1 1 0
	       0 1 0 0
	       0 0 0 0]]
      :stick [[0 1 0 0
	       0 1 0 0
	       0 1 0 0
	       0 1 0 0]
	      [0 0 0 0
	       1 1 1 1
	       0 0 0 0
	       0 0 0 0]]
      :tshape [[0 0 0 0
		0 1 0 0
		1 1 1 0
		0 0 0 0]
	       [0 0 0 0
		0 1 0 0
		1 1 0 0
		0 1 0 0]
	       [0 0 0 0
		0 0 0 0
		1 1 1 0
		0 1 0 0]
	       [0 0 0 0
		0 1 0 0
		0 1 1 0
		0 1 0 0]]}
  "Contrary to its name, this map contains not only the block types, but also
the rotation phases they can take.")

(defvar block-colors
     {:square Color/red
      :lright Color/yellow
      :lleft  Color/green
      :sright Color/magenta
      :sleft  Color/gray
      :stick  Color/cyan
      :tshape Color/blue}
  "Colors associated with the shape types.")

;; The block object."
(defstruct block :type :rotation :position)

(defvar default-block (struct block :square 0 [0 0])
  "A block with default attributes. See get-block.")
(defn get-block
  "Returns a block. Help! How to do this w/o default-block?"
  ([] default-block)
  ([type] (assoc default-block :type type))
  ([type rotation] (assoc default-block :type type, :rotation rotation))
  ([type rotation position] (struct block type rotation position)))
