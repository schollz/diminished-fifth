(ns app.player
  (:require [app.audio :as audio]
            [app.math :as math]
            [app.state :refer [melodies samples]]
            [app.util :refer [log]]))

(def fade-rate 0.01)
(def transpose-on-repeat 2)
(def initial-transposition 1)
(def min-transposition (/ initial-transposition 8))
(def max-transposition (* initial-transposition 8))
(def min-velocity (/ 1 32))
(def max-velocity 16)





(defn get-note-at-index [melody index]
  (nth (:notes melody) index))

(defn get-player-melody [player]
  (nth @melodies (:melody-index player)))

(defn get-melody-at-index [index]
  (nth @melodies index))

(defn get-next-note [player]
  (get-note-at-index (get-player-melody player)
                     (:next-note player)))





(defn get-sync-position [reference-player]
  (if (nil? reference-player)
    0
    (let [duration (:duration (get-player-melody reference-player))]
      (mod
       (/ (mod (+ (:position reference-player) duration)
               duration) ; This mod ensures that we aren't < 0
          (:scale reference-player))
       duration))))

(defn determine-starting-note [melody-index player-position]
  (let [notes (:notes (get-melody-at-index melody-index))
        next-note-index (:index (first (filter #(>= (:position %) player-position) notes)))]
    (or next-note-index 0)))





(defn update-position [player dt velocity]
  (let [position (+ (:position player)
                    (* velocity dt (:scale player)))]
    (assoc player :position position)))

(defn update-dying [player dt velocity]
  (assoc player :dying
         (or (:dying player)
             (< (* velocity (:scale player)) min-velocity)
             (> (* velocity (:scale player)) max-velocity)
             (<= (:transposition player) min-transposition)
             (>= (:transposition player) max-transposition))))

(defn update-volume [player dt]
  (assoc player :volume
         (math/clip
           (if (:dying player)
             (- (:volume player)
                (* dt fade-rate))
             (+ (:volume player)
                (* dt fade-rate))))))

(defn update-alive [player]
  (assoc player :alive
         (or (not (:dying player))
             (> (:volume player) 0))))


; PLAY NOTE ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn update-next-note [player]
  (let [melody (get-player-melody player)
        notes (:notes melody)
        duration (:duration melody)
        next-note (inc (:next-note player))]
    (if (< next-note (count notes))
      (assoc player :next-note next-note)
      (-> player
        (assoc :next-note 0)
        (update :position - duration)
        (update :transposition * transpose-on-repeat)))))

(defn play-note! [player note key-transposition]
  (audio/play (:sample player)
              {:pos (- (:position player) (:position note))
               :pitch (* (:pitch note) (:transposition player) key-transposition)
               :volume (* (:volume player) (/ (:volume note) (:transposition player)))}))

(defn update-played-note [player key-transposition]
  (let [note (get-next-note player)
        player-pos (:position player)
        note-pos (:position note)]
    (if (< player-pos note-pos)
      player
      (do
        (play-note! player note key-transposition)
        (update-next-note player)))))


;; PUBLIC ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn make [reference-player index]
  (let [melody-index (mod index (count @melodies))
        sample-index (mod index (count @samples))
        position (get-sync-position reference-player)]
    {:index index
     :melody-index melody-index
     :sample (nth @samples sample-index)
     :position position ; The current time we're at in the pattern, in ms
     :next-note (determine-starting-note melody-index position)
     :transposition initial-transposition ; Adjusted every time the track repeats by transposeOnRepeat
     :scale 1 ; Adjusted when the Orchestra rescales. Applied to incoming velocity values
     :volume (if (zero? index) 1 0)
     :alive true ; When we die, we'll get filtered out of the list of players
     :dying false}))

(defn tick [player dt velocity key-transposition]
  (-> player
      (update-position dt velocity)
      (update-dying dt velocity)
      (update-volume dt)
      (update-alive)
      (update-played-note key-transposition)))

(defn rescale [player factor]
  (update player :scale * factor))
