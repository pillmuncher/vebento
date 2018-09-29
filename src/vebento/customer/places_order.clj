(ns vebento.customer.places-order
  (:require [clojure.set
             :refer [intersection]]
            [monads.util
             :refer [mwhen]]
            [util
             :refer [ns-alias not-in? intersect?]]
            [juncture.event
             :as event
             :refer [def-command def-failure message handle]]
            [juncture.entity
             :as entity
             :refer [transform transform-in]]
            [componad
             :refer [mdo-within mdo-parallel]]
            [vebento.core
             :refer [boundary publish raise get-entity fail-if-exists
                     update-entity yield*]]))


(ns-alias 'specs 'vebento.specs)
(ns-alias 'order 'vebento.order)
(ns-alias 'merchant 'vebento.merchant)
(ns-alias 'customer 'vebento.customer)


(def-command ::customer/place-order
  :req [::customer/id
        ::order/id])


(def-failure ::customer/cart-is-empty
  :req [::customer/id])


(def-failure ::customer/has-selected-no-merchant
  :req [::customer/id])


(def-failure ::customer/has-selected-no-schedule
  :req [::customer/id])


(def-failure ::customer/has-selected-no-payment-method
  :req [::customer/id])


(defmethod transform
  [::customer/entity ::order/placed]
  [customer {order-id ::order/id}]
  (update customer ::customer/pending-orders conj order-id))


(defmethod handle
  [::component ::order/placed]
  [component event]
  (transform-in (:repository component) ::id event))


(defn subscriptions
  [component]

  {::order/placed
   [(transform-in (:repository component) ::customer/id)]

   ::customer/place-order
   [(fn [{customer-id ::customer/id
          order-id ::order/id}]
      (mdo-within (boundary component #{::customer/shop})
        (fail-if-exists ::order/id order-id)
        customer <- (get-entity ::customer/id customer-id)
        (mdo-parallel
          (mwhen (-> @customer ::customer/cart empty?)
                 (raise ::customer/cart-is-empty
                        ::customer/id customer-id))
          (mwhen (-> @customer ::merchant/id nil?)
                 (raise ::customer/has-selected-no-merchant
                        ::customer/id customer-id))
          (mwhen (-> @customer ::customer/address nil?)
                 (raise ::customer/has-given-no-address
                        ::customer/id customer-id))
          (mwhen (-> @customer ::customer/schedule empty?)
                 (raise ::customer/has-selected-no-schedule
                        ::customer/id customer-id))
          (mwhen (-> @customer ::customer/payment-method nil?)
                 (raise ::customer/has-selected-no-payment-method
                        ::customer/id customer-id)))
        merchant <- (get-entity ::merchant/id (@customer ::merchant/id))
        (mdo-parallel
          (mwhen (empty? (intersection
                           (@customer ::customer/schedule)
                           (@merchant ::merchant/schedule)))
                 (raise ::customer/schedule-not-in-merchant-schedule
                        ::customer/id customer-id
                        ::customer/schedule (@customer
                                              ::customer/schedule)))
          (mwhen (-> @customer ::customer/payment-method
                     (not-in? (@merchant ::merchant/payment-methods)))
                 (raise ::merchant/does-not-support-payment-method
                        ::merchant/id (@merchant ::merchant/id)
                        ::merchant/payment-method (@customer
                                                    ::customer/payment-method)))
          (mwhen (-> @customer ::customer/address ::specs/zipcode
                     (not-in? (@merchant ::merchant/areas)))
                 (raise ::customer/zipcode-not-in-merchant-areas
                        ::customer/id customer-id
                        ::specs/zipcode (-> @customer
                                            ::customer/address
                                            ::specs/zipcode))))
        (yield*
          (message ::order/placed
                  ::customer/id customer-id
                  ::merchant/id (@customer ::merchant/id)
                  ::order/id order-id
                  ::order/items (@customer ::customer/cart)
                  ::order/address (@customer ::customer/address)
                  ::order/payment-method (@customer ::customer/payment-method)
                  ::order/schedule (intersection
                                      (@customer ::customer/schedule)
                                      (@merchant ::merchant/schedule)))
          (message ::customer/cart-cleared
                  ::customer/id customer-id))))]})
