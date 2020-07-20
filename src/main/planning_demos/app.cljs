;(shadow.cljs.devtools.api/nrepl-select :app)
(ns planning-demos.app
  (:require [planning.core :as p]
            [planning.utils :as u]
            [reagent.core :as r]
            [reagent.dom :as rd]))

;https://github.com/reagent-project/reagent
;https://reagent-project.github.io
;https://developer.mozilla.org/en-US/docs/Web/SVG/Attribute/Events
;https://developer.mozilla.org/en-US/docs/Web/API/Touch_events
;https://reactjs.org/docs/events.html#touch-events
;https://www.javascripture.com/Touch

(def cost '{🌲 1 🌳 1.5 🌴 2 ⛰ 5 🌋 10})

(def cell-dim 24)
(def grid-size 12)

(def state (r/atom {:grid (vec (repeat grid-size (vec (repeat grid-size '🌲))))}))

(defn A-star-meadow-search [{:keys [grid start goal] :as m}]
  (when (and start goal)
    (p/A-star-search
      (assoc m
        :neighbors-fn (partial u/moore-neigbors grid)
        :cost-fn (fn [f t] (* (cost (get-in grid t) ##Inf) (u/euclidian-distance f t)))
        :heuristic-fn u/euclidian-distance))))

(defn render-path [state]
  (doall
    (for [[i j] (A-star-meadow-search @state)]
      [:circle {:key    (str "step:" i ":" j)
                :cx     (* (+ i 0.5) cell-dim)
                :cy     (* (+ j 0.5) cell-dim)
                :r      (* cell-dim 0.5)
                :stroke :black
                :fill   :gray}])))

(defn render-goal [state state-key emoji]
  (let [goal (r/cursor state [state-key])]
    (fn []
      (let [[i j] @goal]
        (when @goal
          [:text {:x       (+ (* i cell-dim) (* 0.125 cell-dim))
                  :y       (- (* (inc j) cell-dim) (* 0.25 cell-dim))
                  :onClick #(swap! state dissoc state-key)}
           emoji])))))

(defn add-paintbrush [state brush]
  (let [paintbrush (r/cursor state [:paintbrush])
        x (double (/ (* cell-dim grid-size) (count cost)))]
    (fn []
      [:svg {:width x :height cell-dim}
       [:rect {:width x :height cell-dim
               :fill  (if (= brush @paintbrush) :gray :white)}]
       [:text {:x       (* 0.5 (- x cell-dim))
               :y       (* 0.75 cell-dim)
               :onClick #(if-not (= brush @paintbrush)
                           (reset! paintbrush brush)
                           (swap! state dissoc :paintbrush))}
        (str brush)]])))

(defn render [state]
  (let [grid (r/cursor state [:grid])
        paintbrush (r/cursor state [:paintbrush])
        dragging (r/atom false)
        w (count @grid)
        h (count (first @grid))]
    (fn []
      ;https://stackoverflow.com/questions/9251590/prevent-page-scroll-on-drag-in-ios-and-android
      [:div.no-bounce {:style {:cursor :pointer :user-select :none}}
       [:svg#grid {:width        (* cell-dim w)
                   :height       (* cell-dim h)
                   :onTouchStart #(reset! dragging true)
                   :onTouchEnd   #(reset! dragging false)
                   :onMouseDown  #(reset! dragging true)
                   :onMouseUp    #(reset! dragging false)
                   :onMouseLeave #(reset! dragging false)}
        [:rect {:width (* w cell-dim) :height (* h cell-dim) :fill :green}]
        (render-path state)
        (doall
          (for [i (range w) j (range h) :let [c (get-in @grid [i j])]]
            [:text {:key         (str i ":" j)
                    :x           (+ (* i cell-dim) (* 0.125 cell-dim))
                    :y           (- (* (inc j) cell-dim) 6)
                    :data-i      i
                    :data-j      j
                    :onMouseOver #(when (and @dragging @paintbrush)
                                    (swap! grid assoc-in [i j] @paintbrush))
                    :onTouchMove (fn [event]
                                   (when (and @dragging @paintbrush)
                                     (let [touch (.item (.-touches event) 0)
                                           dataset (.-dataset (. js/document
                                                                 (elementFromPoint
                                                                   (.-clientX touch)
                                                                   (.-clientY touch))))
                                           i (some-> dataset .-i js/parseInt)
                                           j (some-> dataset .-j js/parseInt)]
                                       (when (and i j)
                                         (swap! grid assoc-in [i j] @paintbrush)))))
                    :onClick     #(cond
                                    @paintbrush (swap! grid assoc-in [i j] @paintbrush)
                                    (= [i j] (:start @state)) (swap! state dissoc :start)
                                    (= [i j] (:goal @state)) (swap! state dissoc :goal)
                                    (:start @state) (swap! state assoc :goal [i j])
                                    :default (swap! state assoc :start [i j]))}
             c]))
        [render-goal state :start '🧝]
        [render-goal state :goal '🗃️]]
       [:div
        [:span
         (doall (for [k ['🌲 '🌳 '🌴 '⛰ '🌋]]
                  ^{:key k} [add-paintbrush state k]))]]])))

(defn ^:dev/after-load ui-root []
  (rd/render [render state] (.getElementById js/document "ui-root")))

(defn init []
  (let [root (.getElementById js/document "ui-root")]
    (.log js/console root)
    (rd/render [render state] root)))