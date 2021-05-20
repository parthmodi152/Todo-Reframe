(ns todo-reframe.db
  (:require [cljs.reader]
            [cljs.spec.alpha :as s]
            [re-frame.core :as re-frame]))

;; ----- Schema ----------------------------------------------------------

(s/def ::id int?)
(s/def ::username string?)
(s/def ::logged-in boolean?)
(s/def ::auth-error (s/nilable string?))
(s/def ::title string?)
(s/def ::done boolean?)
(s/def ::todo (s/keys :req-un [::id ::title ::done]))
(s/def ::todos (s/map-of ::id ::todo))
(s/def ::showing
  #{:all
    :active
    :done})
(s/def ::db (s/keys :req-un [::todos ::showing ::logged-in ::auth-error]))


;; ----- Default DB ----------------------------------------------------------

(def default-db
  {:todos (sorted-map)
   :showing :all
   :logged-in false
   :auth-error nil})

;; (def default-ls
;;   {:logged-in false
;;    :current-user nil
;;    :users {}})

;; -- Local Storage  ----------------------------------------------------------

(def ls-key "users-reframe")

(defn get-ls-db
  []
  (into (sorted-map)
        (some->> (.getItem js/localStorage ls-key)
                 (cljs.reader/read-string)
                 )))

(defn set-ls
  [ls]
  (.setItem js/localStorage ls-key (str ls)))

(defn todos->local-store
  [todos]
  (let [curr-ls (get-ls-db)
        logged-in (:logged-in curr-ls)]
    (if logged-in
      (set-ls 
       (assoc-in curr-ls 
                      [:users (:current-user curr-ls) :todos]
                      todos))
      curr-ls)))

(defn user->local-store
  [name username password]
  (let [curr-ls (get-ls-db)]
    (set-ls (assoc-in curr-ls
                      [:users username]
                      {:name name
                       :password password
                       :todos (sorted-map)}))))

(defn login->local-store
  [username]
  (let [curr-ls (get-ls-db)]
    (set-ls 
     (assoc (assoc curr-ls :logged-in true) :current-user username))))

(defn logout->local-store
  []
  (let [curr-ls (get-ls-db)]
    (set-ls 
     (assoc (assoc curr-ls :logged-in false) :current-user nil))))

;; -- cofx Registrations  -----------------------------------------------------

(re-frame/reg-cofx
 :local-store
 (fn [cofx _]
   (assoc cofx :local-store
         (get-ls-db))))



