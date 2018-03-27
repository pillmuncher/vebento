(ns vebento.customer.places-order
  (:require [clojure.set
             :refer [intersection]]
            [monads.util
             :refer [mwhen]]
            [util
             :refer [ns-alias not-in? intersect?]]
            [juncture.event
             :as event
             :refer [def-command def-error handle]]
            [juncture.entity
             :as entity
             :refer [transform transform-in]]
            [componad
             :refer [mdo-within mdo-parallel]]
            [vebento.core
             :refer [boundary publish raise get-entity fail-if-exists
                     update-entity]]))


(ns-alias 'specs 'vebento.specs)
(ns-alias 'order 'vebento.order)
(ns-alias 'merchant 'vebento.merchant)
(ns-alias 'customer 'vebento.customer)


(def-command ::customer/place-order
  :req [::customer/id
        ::order/id])


(def-error ::customer/cart-is-empty
  :req [::customer/id])


(def-error ::customer/has-selected-no-merchant
  :req [::customer/id])


(def-error ::customer/has-selected-no-schedule
  :req [::customer/id])


(def-error ::customer/has-selected-no-payment-method
  :req [::customer/id])


(defmethod transform
  [::customer/entity ::order/placed]
  [customer {order-id ::order/id}]
  (update customer ::customer/pending-orders conj order-id))


(defmethod handle
  [::component ::order/placed]
  [component event]
  (transform-in (:repository component) ::id event))


(defmethod handle
  [::component ::place-order]
  [component {customer-id ::id
              order-id ::order/id}]
  (mdo-within (boundary component customer-id #{::shop})
    (fail-if-exists ::order/id order-id)
    customer <- (get-entity ::id customer-id)
    (mdo-parallel
      (mwhen (-> @customer ::cart empty?)
             (raise ::cart-is-empty
                    ::id customer-id))
      (mwhen (-> @customer ::merchant/id nil?)
             (raise ::has-selected-no-merchant
                    ::id customer-id))
      (mwhen (-> @customer ::address nil?)
             (raise ::has-given-no-address
                    ::id customer-id))
      (mwhen (-> @customer ::schedule empty?)
             (raise ::has-selected-no-schedule
                    ::id customer-id))
      (mwhen (-> @customer ::payment-method nil?)
             (raise ::has-selected-no-payment-method
                    ::id customer-id)))
    merchant <- (get-entity ::merchant/id (@customer ::merchant/id))
    (mdo-parallel
      (mwhen (empty? (intersection (@customer ::schedule)
                                   (@merchant ::merchant/schedule)))
             (raise ::schedule-not-in-merchant-schedule
                    ::id customer-id
                    ::schedule (@customer ::schedule)))
      (mwhen (-> @customer ::payment-method
                 (not-in? (@merchant ::merchant/payment-methods)))
             (raise ::merchant/does-not-support-payment-method
                    ::merchant/id (@merchant ::merchant/id)
                    ::merchant/payment-method (@customer ::payment-method)))
      (mwhen (-> @customer ::address ::specs/zipcode
                 (not-in? (@merchant ::merchant/areas)))
             (raise ::zipcode-not-in-merchant-areas
                    ::id customer-id
                    ::specs/zipcode (-> @customer
                                        ::address
                                        ::specs/zipcode))))
    (publish ::order/placed
             ::id customer-id
             ::merchant/id (@customer ::merchant/id)
             ::order/id order-id
             ::order/items (@customer ::cart)
             ::order/address (@customer ::address)
             ::order/payment-method (@customer ::payment-method)
             ::order/schedule (intersection
                                (@customer ::schedule)
                                (@merchant ::merchant/schedule)))
    (publish ::cart-cleared
             ::id customer-id)))


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
        (publish ::order/placed
                 ::customer/id customer-id
                 ::merchant/id (@customer ::merchant/id)
                 ::order/id order-id
                 ::order/items (@customer ::customer/cart)
                 ::order/address (@customer ::customer/address)
                 ::order/payment-method (@customer ::customer/payment-method)
                 ::order/schedule (intersection
                                    (@customer ::customer/schedule)
                                    (@merchant ::merchant/schedule)))
        (publish ::customer/cart-cleared
                 ::customer/id customer-id)))]})
