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

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Parent;

import google.registry.model.common.EntityGroupRoot;
import google.registry.model.JsonMapBuilder;
import google.registry.model.Buildable;
import google.registry.model.ImmutableObject;
import google.registry.model.Jsonifiable;


import org.joda.money.Money;
import org.joda.time.DateTime;

import java.util.Map;
import java.util.Objects;

/** A Pricing Category for a premium domain if premiumListEntry lists
 a domain as in a specific price category, then pricing for that domain
will be deferred to this category*/ 
 

@Cache(expirationSeconds = RECOMMENDED_MEMCACHE_EXPIRATION)
@Entity
public class PricingCategory extends ImmutableObject implements Jsonifiable {

    @Parent
    Key<EntityGroupRoot> parent = getCrossTldKey();

    /**
     * Universally unique name for this price category.   
     */
    @Id
    String pricingCategoryName;

    Money createPriceFirstYear;

    Money createPriceRemainingYears;

    Money renewPrice;

    Money transferPrice;

    Money restorePrice;

    DateTime effectiveDate;

    DateTime endDate;

    @Override
    public Map<String, Object> toJsonMap() {
        return new JsonMapBuilder()
            .put("pricingCategoryName", pricingCategoryName)
            .put("createPriceFirstYear", createPriceFirstYear.getAmount())
            .put("createPriceRemainingYears", createPriceRemainingYears.getAmount())
            .put("renewPrice", renewPrice.getAmount())
            .put("transferPrice", transferPrice.getAmount())
            .put("restorePrice", restorePrice.getAmount())
            .put("effectiveDate", effectiveDate.toString())
            .put("endDate", endDate.toString())
            .build();

    }

    public String getPricingCategoryName() {
        return pricingCategoryName;
    }
    public Money getCreatePriceFirstYear() {
        return createPriceFirstYear;
    }
    public Money getCreatePriceRemainingYears() {
        return createPriceRemainingYears;
    }
    public Money getRenewPrice() {
        return renewPrice;
    }
    public Money getTransferPrice() {
        return transferPrice;
    }
    public Money getRestorePrice() {
        return restorePrice;
    }
    public DateTime getEffectiveDate() {
        return effectiveDate;
    }
    public DateTime getEndDate() {
        return endDate;
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
        public Builder setCreatePriceFirstYear(Money createPriceFirstYear) {
            getInstance().createPriceFirstYear = createPriceFirstYear;
            return this; 
        }
        public Builder setCreatePriceRemainingYears(Money createPriceRemainingYears) {
            getInstance().createPriceRemainingYears = createPriceRemainingYears;
            return this; 
        }
        public Builder setRenewPrice(Money renewPrice) {
            getInstance().renewPrice = renewPrice;
            return this; 
        }
        public Builder setTransferPrice(Money transferPrice) {
            getInstance().transferPrice = transferPrice;
            return this; 
        }
        public Builder setRestorePrice(Money restorePrice) {
            getInstance().restorePrice = restorePrice;
            return this; 
        }
        public Builder setEffectiveDate(DateTime effectiveDate) {
            getInstance().effectiveDate = effectiveDate;
            return this; 
        }
        public Builder setEndDate(DateTime endDate) {
            getInstance().endDate = endDate;
            return this; 
        }


        public Builder setIfNotNullPriceCategoryName(String pricingCategoryName) {
            if(pricingCategoryName != null) {
                getInstance().pricingCategoryName = pricingCategoryName;
            }
            return this;
        }
        public Builder setIfNotNullCreatePriceFirstYear(Money createPriceFirstYear) {
            if(createPriceFirstYear != null) {
                getInstance().createPriceFirstYear = createPriceFirstYear;
            }
            return this;
        }
        public Builder setIfNotNullCreatePriceRemainingYears(Money createPriceRemainingYears) {
            if(createPriceRemainingYears != null) {
                getInstance().createPriceRemainingYears = createPriceRemainingYears;
            }
            return this;
        }
        public Builder setIfNotNullRenewPrice(Money renewPrice) {
            if(renewPrice != null) {
                getInstance().renewPrice = renewPrice;
            }
            return this;
        }
        public Builder setIfNotNullTransferPrice(Money transferPrice) {
            if(transferPrice != null) {
                getInstance().transferPrice = transferPrice;
            }
            return this;
        }
        public Builder setIfNotNullRestorePrice(Money restorePrice) {
            if(restorePrice != null) {
                getInstance().restorePrice = restorePrice;
            }
            return this;
        }
        public Builder setIfNotNullEffectiveDate(DateTime effectiveDate) {
            if(effectiveDate != null) {
                getInstance().effectiveDate = effectiveDate;
            }
            return this;
        }
        public Builder setIfNotNullEndDate(DateTime endDate) {
            if(endDate != null) {
                getInstance().endDate = endDate;
            }
            return this;
        }


        public PricingCategory build() {
            final PricingCategory instance = getInstance();

            checkArgument((instance.getPricingCategoryName() != null), "pricingCategoryName cannot be null");
            checkArgument((instance.getCreatePriceFirstYear() != null), "createPriceFirstYear cannot be null");
            checkArgument((instance.getCreatePriceRemainingYears() != null), "createPriceRemainingYears cannot be null");
            checkArgument((instance.getRenewPrice() != null), "renewPrice cannot be null");
            checkArgument((instance.getTransferPrice() != null), "transferPrice cannot be null");
            checkArgument((instance.getRestorePrice() != null), "restorePrice cannot be null");
            checkArgument((instance.getEffectiveDate() != null), "effectiveDate cannot be null");
            checkArgument((instance.getEndDate() != null), "endDate cannot be null");

            return super.build();
        }


    }


    public static class PricingCategoryNotFoundException extends RuntimeException{
        PricingCategoryNotFoundException(String cat) {
            super("No pricing category named '" + cat + "' was found.");
        }
    }






}
