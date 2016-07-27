// Copyright 2016 The Domain Registry Authors. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package google.registry.model.pricing;

import static com.google.common.base.Preconditions.checkArgument;
import static google.registry.model.ofy.Ofy.RECOMMENDED_MEMCACHE_EXPIRATION;
import static google.registry.model.common.EntityGroupRoot.getCrossTldKey;

import com.google.common.collect.ImmutableSortedMap;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Embed;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Mapify;
import com.googlecode.objectify.annotation.Parent;

import google.registry.model.common.EntityGroupRoot;
import google.registry.model.common.TimedTransitionProperty;
import google.registry.model.common.TimedTransitionProperty.TimedTransition;
import google.registry.model.JsonMapBuilder;
import google.registry.model.Buildable;
import google.registry.model.ImmutableObject;
import google.registry.model.Jsonifiable;


import org.joda.time.DateTime;

import java.util.Map;
import java.util.Objects;

/** A Pricing Category for a premium domain if premiumListEntry lists
 a domain as in a specific price category, then pricing for that domain
will be deferred to this category*/ 
 

@Cache(expirationSeconds = RECOMMENDED_MEMCACHE_EXPIRATION)
@Entity
public class PricingCategory extends ImmutableObject {

    @Parent
    Key<EntityGroupRoot> parent = getCrossTldKey();

    /**
     * Universally unique name for this price category.
     */
    @Id
    String pricingCategoryName;

    String comments;

    /**
     * A single transition event to set of domain prices at a specific time, for use
     * in a TimedTransitionProperty.
     *
     * Public because AppEngine's security manager requires this for instantiation via reflection.
     */
    @Embed
    public static class DomainPricesTransition extends TimedTransition<DomainPrices> {
        private DomainPrices prices;

        @Override
        public DomainPrices getValue() {
            return prices;
        }

        @Override
        protected void setValue(DomainPrices prices) {
            this.prices = prices; 
        }
    }
    

    /**
     * A property that transitions to different domainPrices at different times. Stored as a list of
     * domainPrices Transition embedded objects using the @Mapify annotation.
     */
    @Mapify(TimedTransitionProperty.TimeMapper.class)
    TimedTransitionProperty<DomainPrices, DomainPricesTransition> priceTransitions =
        TimedTransitionProperty.forMapify(new DomainPrices(), DomainPricesTransition.class);

    public String getPricingCategoryName() {
        return pricingCategoryName;
    }
    public ImmutableSortedMap<DateTime, DomainPrices> getPriceTransitions() {
        return priceTransitions.toValueMap();
    }
    public String getComments() {
        return comments;
    }

    public static class Builder extends Buildable.Builder<PricingCategory> {
        public Builder() {}

        public Builder(PricingCategory instance) {
            super(instance);

        }

        public Builder setPricingCategoryName(String pricingCategoryName) {
            getInstance().pricingCategoryName = pricingCategoryName;
            return this;
        }
        public Builder setPriceTransitions(
            Map<DateTime, DomainPrices> priceTransitionsMap) {

            getInstance().priceTransitions =
                TimedTransitionProperty.fromValueMap(ImmutableSortedMap.copyOf(priceTransitionsMap), DomainPricesTransition.class);
            return this;
        }
        public Builder setComments(String comments) {
            getInstance().comments = comments;
            return this;
        }

        public Builder setIfNotNullPriceCategoryName(String pricingCategoryName) {
            if(pricingCategoryName != null) {
                getInstance().pricingCategoryName = pricingCategoryName;
            }
            return this;
        }
        public Builder setIfNotNullPriceTransitions(
                                           Map<DateTime, DomainPrices> priceTransitionsMap) {

            if(priceTransitionsMap != null) {
                getInstance().priceTransitions =
                    TimedTransitionProperty.fromValueMap(ImmutableSortedMap.copyOf(priceTransitionsMap), DomainPricesTransition.class);
            }
            return this;
        }
        public Builder setIfNotNullComments(String comments) {
            if(comments != null) {
                getInstance().comments = comments;
            }
            return this;
        }

        public PricingCategory build() {
            final PricingCategory instance = getInstance();

            checkArgument((instance.getPricingCategoryName() != null), "pricingCategoryName cannot be null");
            checkArgument((instance.getPriceTransitions() != null), "priceTransitions cannot be null");

            return super.build();
        }

    }


    public static class PricingCategoryNotFoundException extends RuntimeException{
        PricingCategoryNotFoundException(String cat) {
            super("No pricing category named '" + cat + "' was found.");
        }
    }


}
