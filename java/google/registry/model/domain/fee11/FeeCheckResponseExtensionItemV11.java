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

package google.registry.model.domain.fee11;

import google.registry.model.domain.DomainObjectSpec;
import google.registry.model.domain.fee.FeeCheckResponseExtensionItem;
import google.registry.model.domain.fee.FeeQueryResponseExtensionItemImpl;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import org.joda.money.CurrencyUnit;
import org.joda.time.DateTime;

/** The version 0.11 response for a domain check on a single resource. */
@XmlType(propOrder = {"object", "command", "currency", "period", "fee", "feeClass", "reason"})
public class FeeCheckResponseExtensionItemV11
    extends FeeQueryResponseExtensionItemImpl implements FeeCheckResponseExtensionItem {

  /** Whether the domain is available. */
  @XmlAttribute
  boolean avail;

  /** The domain that was checked. */
  DomainObjectSpec object;
  
  CurrencyUnit currency;

  /** The reason that the check item cannot be calculated. */
  String reason;
  
  /** Builder for {@link FeeCheckResponseExtensionItemV11}. */
  public static class Builder
      extends FeeQueryResponseExtensionItemImpl.Builder<FeeCheckResponseExtensionItemV11, Builder>
      implements FeeCheckResponseExtensionItem.Builder {
    
    @Override
    public Builder setDomainNameIfSupported(String name) {
      getInstance().object = new DomainObjectSpec(name);
      return this;
    }
    
    @Override
    public Builder setCurrencyIfSupported(CurrencyUnit currency) {
      getInstance().currency = currency;
      return this;
    }
        
    @Override
    public Builder setAvailIfSupported(boolean avail) {
      getInstance().avail = avail;
      return this;
    }
    
    @Override
    public Builder setReasonIfSupported(String reason) {
      getInstance().reason = reason;
      return this;
    }

    @Override
    public Builder setEffectiveDateIfSupported(DateTime effectiveDate) {
      return this;
    }

    @Override
    public Builder setNotAfterDateIfSupported(DateTime notAfterDate) {
      return this;
    }
  }
}
