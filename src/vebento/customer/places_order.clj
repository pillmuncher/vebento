(ns vebento.customer.places-order
  (:require [clojure.set
             :refer [intersection]]
            [monads.util
             :refer [mwhen]]
            [util
             :refer [ns-alias not-in?]]
            [juncture.event
             :as event
             :refer [defcommand deffailure]]
            [juncture.entity
             :as entity
             :refer [mutate mutate-in]]
            [componad
             :refer [mdo-within mdo-parallel]]
            [vebento.core
             :refer [boundary get-entity fail-if-exists call post fail]]))


(ns-alias 'specs 'vebento.specs)
(ns-alias 'order 'vebento.order)
(ns-alias 'merchant 'vebento.merchant)
(ns-alias 'customer 'vebento.customer)


(defcommand ::customer/place-order
  :req [::customer/id
        ::order/id])


(deffailure ::customer/cart-is-empty
  :req [::customer/id])


(deffailure ::customer/has-selected-no-merchant
  :req [::customer/id])


(deffailure ::customer/has-selected-no-schedule
  :req [::customer/id])


(deffailure ::customer/has-selected-no-payment-method
  :req [::customer/id])


(defmethod mutate
  [::customer/entity ::order/placed]
  [customer {order-id ::order/id}]
  (update customer ::customer/pending-orders conj order-id))


(defn subscriptions
  [component]

  {::order/placed
   [(mutate-in (:repository component) ::customer/id)]

   ::customer/place-order
   [(fn [{customer-id ::customer/id
          order-id ::order/id}]
      (mdo-within (boundary component #{::customer/shop})
        (fail-if-exists ::order/id order-id)
        customer <- (get-entity ::customer/id customer-id)
        (mdo-parallel
          (mwhen (-> @customer ::customer/cart empty?)
                 (fail ::customer/cart-is-empty
                       ::customer/id customer-id))
          (mwhen (-> @customer ::merchant/id nil?)
                 (fail ::customer/has-selected-no-merchant
                       ::customer/id customer-id))
          (mwhen (-> @customer ::customer/address nil?)
                 (fail ::customer/has-given-no-address
                       ::customer/id customer-id))
          (mwhen (-> @customer ::customer/schedule empty?)
                 (fail ::customer/has-selected-no-schedule
                       ::customer/id customer-id))
          (mwhen (-> @customer ::customer/payment-method nil?)
                 (fail ::customer/has-selected-no-payment-method
                       ::customer/id customer-id)))
        merchant <- (get-entity ::merchant/id (@customer ::merchant/id))
        (mdo-parallel
          (mwhen (empty? (intersection (@customer ::customer/schedule)
                                       (@merchant ::merchant/schedule)))
                 (fail ::customer/schedule-not-in-merchant-schedule
                       ::customer/id customer-id
                       ::customer/schedule (@customer ::customer/schedule)))
          (mwhen (-> @customer ::customer/payment-method
                     (not-in? (@merchant ::merchant/payment-methods)))
                 (fail ::merchant/does-not-support-payment-method
                       ::merchant/id (@merchant ::merchant/id)
                       ::merchant/payment-method
                       (@customer ::customer/payment-method)))
          (mwhen (-> @customer ::customer/address ::specs/zipcode
                     (not-in? (@merchant ::merchant/areas)))
                 (fail ::customer/zipcode-not-in-merchant-areas
                       ::customer/id customer-id
                       ::specs/zipcode (-> @customer
                                           ::customer/address
                                           ::specs/zipcode))))
        (post ::order/placed
              ::customer/id customer-id
              ::merchant/id (@customer ::merchant/id)
              ::order/id order-id
              ::order/items (@customer ::customer/cart)
              ::order/address (@customer ::customer/address)
              ::order/payment-method (@customer ::customer/payment-method)
              ::order/schedule (intersection (@customer ::customer/schedule)
                                             (@merchant ::merchant/schedule)))
        (call ::customer/clear-cart
              ::customer/id customer-id)))]})
