(ns todo-reframe.events
  (:require
   [re-frame.core :refer [reg-event-db reg-event-fx reg-fx inject-cofx path after]]
   [todo-reframe.db :as db]
   [cljs.spec.alpha :as s]
   ))


;; ----- Interceptors --------------------------------------------------

(defn check-and-throw
  "Throws an exception if `db` doesn't match the Spec `a-spec`."
  [a-spec db]
  (when-not (s/valid? a-spec db)
    (throw (ex-info (str "spec check failed: " (s/explain-str a-spec db)) {}))))

;; now we create an interceptor using `after`
(def check-spec-interceptor (after (partial check-and-throw :todo-reframe.db/db)))

(def ->local-store (after db/todos->local-store))

(def todo-interceptors [check-spec-interceptor
                        (path :todos)
                        ->local-store])

;; -- Helpers -----------------------------------------------------------------

(defn allocate-next-id
  "Returns the next todo id.
  Assumes todos are sorted.
  Returns one more than the current largest id."
  [todos]
  ((fnil inc 0) (last (keys todos))))


;; -- Event Handlers ----------------------------------------------------------


;; DB Initializer
(reg-event-fx
 :initialise-db
 [(inject-cofx :local-store)
  check-spec-interceptor]
 (fn [{:keys [db local-store]} _]
   (if (:logged-in local-store)
     {:db {:todos (get-in local-store 
                          [:users 
                           (:current-user local-store) 
                           :todos])
           :showing :all
           :logged-in true
           :auth-error nil}}
     {:db db/default-db})))

(reg-fx
 :ls
 (fn [default-ls]
   (db/set-ls default-ls)))

;; Handles the Login Event

(reg-fx
 :login-user
 (fn [username]
   (db/login->local-store username)))

(reg-event-fx
 :login
 [(inject-cofx :local-store)
  check-spec-interceptor]
 (fn [{:keys [db local-store]} 
      [_ {:keys [username password]}]]
   (println local-store)
   (println (:users local-store))
   (if (contains? (:users local-store) username)
     (if (= (get-in local-store [:users username :password]) password)
       {:db (assoc (assoc (assoc db :logged-in true) :auth-error nil) :todos 
                   (get-in local-store [:users username :todos]))
        :login-user username}
       {:db (assoc db :auth-error "Incorrect Password")})
     {:db (assoc db :auth-error "Username not found")})))


;; Handles the SignIn Event

(reg-fx
 :new-user
 (fn [{:keys [name username password]}]
   (db/user->local-store name username password)))

(reg-event-fx
 :signIn
 [(inject-cofx :local-store)
  check-spec-interceptor]
 (fn [{:keys [db local-store]}
      [_ data]]
   (if (contains? (:users local-store) (:username data))
     {:db (assoc db :auth-error "Username Already Exists")}
     {:db (assoc db :auth-error nil)
      :new-user data})))

;; Handles the LogOut Event

(reg-event-fx
 :logout
 [check-spec-interceptor]
 (fn [{:keys [db]} _]
   {:db (assoc (assoc db :logged-in false) :todos (sorted-map))
    :logout-user nil}))

(reg-fx
 :logout-user
 (fn [_]
   (db/logout->local-store)))

;; Other Todo Add, Update and Delete Event Handlers

(reg-event-db
 :set-showing
 [check-spec-interceptor]
 (fn [db [_ new-filter-kw]]
   (assoc db :showing new-filter-kw)))

(reg-event-db
 :clear-auth-error
 [check-spec-interceptor]
 (fn [db _]
   (assoc db :auth-error nil)))

(reg-event-db
 :add-todo
 todo-interceptors
(fn [todos [_ text]]
    (let [id (allocate-next-id todos)]
      (assoc todos id {:id id :title text :done false}))))

(reg-event-db
 :toggle-done
 todo-interceptors
 (fn [todos [_ id]]
   (update-in todos [id :done] not)))

(reg-event-db
 :save
 todo-interceptors
 (fn [todos [_ id title]]
   (assoc-in todos [id :title] title)))

(reg-event-db
 :delete-todo
 todo-interceptors
 (fn [todos [_ id]]
   (dissoc todos id)))

(reg-event-db
 :clear-completed
 todo-interceptors
 (fn [todos _]
   (let [done-ids (->> (vals todos)        
                       (filter :done)
                       (map :id))]
     (reduce dissoc todos done-ids))))

(reg-event-db
 :complete-all-toggle
 todo-interceptors
 (fn [todos _]
   (let [new-done (not-every? :done (vals todos))]
     (reduce #(assoc-in %1 [%2 :done] new-done)
             todos
             (keys todos)))))
