(ns vebento.customer.places-order
  (:require [clojure.future
             :refer :all]
            [clojure.set
             :refer [intersection]]
            [monads.util
             :refer [mwhen]]
            [util
             :refer [ns-alias not-in?]]
            [juncture.event
             :as event
             :refer [def-command def-failure]]
            [juncture.entity
             :as entity
             :refer [transform transform-in]]
            [componad
             :refer [within mdo-await*]]
            [vebento.core
             :refer [boundary publish fail-with get-entity fail-if-exists]]))


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


(defn subscriptions
  [component]

  {::order/placed
   [(transform-in (:repository component) ::customer/id)]

   ::customer/place-order
   [(fn [{customer-id ::customer/id
          order-id ::order/id}]
      (within (boundary component #{::customer/shop})
        (fail-if-exists ::order/id order-id)
        customer <- (get-entity ::customer/id customer-id)
        (mdo-await*
          (mwhen (-> @customer ::customer/cart empty?)
                 (fail-with ::customer/cart-is-empty
                            ::customer/id customer-id))
          (mwhen (-> @customer ::merchant/id nil?)
                 (fail-with ::customer/has-selected-no-merchant
                            ::customer/id customer-id))
          (mwhen (-> @customer ::customer/address nil?)
                 (fail-with ::customer/has-given-no-address
                            ::customer/id customer-id))
          (mwhen (-> @customer ::customer/schedule empty?)
                 (fail-with ::customer/has-selected-no-schedule
                            ::customer/id customer-id))
          (mwhen (-> @customer ::customer/payment-method nil?)
                 (fail-with ::customer/has-selected-no-payment-method
                            ::customer/id customer-id)))
        merchant <- (get-entity ::merchant/id (@customer ::merchant/id))
        (mdo-await*
          (mwhen (distinct? (@customer ::customer/schedule)
                            (@merchant ::merchant/schedule))
                 (fail-with ::customer/schedule-not-in-merchant-schedule
                            ::customer/id customer-id
                            ::customer/schedule (@customer
                                                  ::customer/schedule)))
          (mwhen (-> @customer ::customer/payment-method
                     (not-in? (@merchant ::merchant/payment-methods)))
                 (fail-with ::merchant/does-not-support-payment-method
                            ::merchant/id (@merchant ::merchant/id)
                            ::merchant/payment-method
                              (@customer ::customer/payment-method)))
          (mwhen (-> @customer ::customer/address ::specs/zipcode
                     (not-in? (@merchant ::merchant/areas)))
                 (fail-with ::customer/zipcode-not-in-merchant-areas
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
