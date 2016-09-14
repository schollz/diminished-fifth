(ns app.state)

(defonce state (atom {}))

(defonce samples (atom {}))
(defonce melodies (atom {}))

(defonce callback (atom nil))

(defonce text-context (atom nil))
(defonce line-context (atom nil))

(defonce history (atom {}))
(defonce history-min (atom {}))
(defonce history-max (atom {}))
                      
(defonce audio-context (atom nil))
(defonce master (atom nil))
(defonce sample-rate (atom nil))

(defonce buffers (atom {}))
