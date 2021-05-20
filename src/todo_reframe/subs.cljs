(ns todo-reframe.subs
  (:require
   [re-frame.core :refer [reg-sub subscribe]]
   [todo-reframe.db]))

(reg-sub
 :showing        
 (fn [db _]
   (:showing db)))

(reg-sub
 :auth-error
 (fn [db _]
   (:auth-error db)))

(reg-sub
 :todos
 (fn [db _]
   (:todos db)))

(reg-sub 
 :logged-in
 (fn [_ _]
   (let [curr-ls (todo-reframe.db/get-ls-db)]
     (:logged-in curr-ls))))

(reg-sub
  :visible-todos
 (fn [_ _]
   [(subscribe [:todos])
    (subscribe [:showing])])
 (fn [[todos showing] _]
    (let [filter-fn (case showing
                      :active #(false? (:done (second %)))
                      :done   #(:done (second %))
                      :all    identity)]
      (filter filter-fn todos))))

(reg-sub
 :all-complete?
 :<- [:todos]
 (fn [todos _]
   (every? #(:done (second %)) todos)))

(reg-sub
 :completed-count
 :<- [:todos]
 (fn [todos _]
   (count (filter #(:done (second %)) todos))))

(reg-sub
 :footer-counts
 :<- [:todos]
 :<- [:completed-count]
 (fn [[todos completed] _]
   [(- (count todos) completed) completed]))