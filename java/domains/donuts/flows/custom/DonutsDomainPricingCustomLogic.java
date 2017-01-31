// Copyright 2016 Donuts Inc. All Rights Reserved.
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

package domains.donuts.flows.custom;

import static domains.donuts.config.DonutsConfigModule.provideDpmlCreateOverridePrice;
import static domains.donuts.config.DonutsConfigModule.provideDpmlLookup;

import domains.donuts.flows.DpmlLookup;
import google.registry.flows.EppException;
import google.registry.flows.FlowMetadata;
import google.registry.flows.SessionMetadata;
import google.registry.flows.custom.DomainPricingCustomLogic;
import google.registry.flows.domain.FeesAndCredits;
import google.registry.model.domain.fee.BaseFee.FeeType;
import google.registry.model.domain.fee.Fee;
import google.registry.model.eppinput.EppInput;
import org.joda.money.Money;

public class DonutsDomainPricingCustomLogic extends DomainPricingCustomLogic {

  // TODO: Dagger inject these. https://groups.google.com/forum/#!topic/nomulus-discuss/4GkhC9naJmU
  private final DpmlLookup dpmlLookup = provideDpmlLookup();
  private final Money overridePrice = provideDpmlCreateOverridePrice();

  DonutsDomainPricingCustomLogic(
    final EppInput eppInput, final SessionMetadata sessionMetadata, final FlowMetadata flowMetadata) {
    super(eppInput, sessionMetadata, flowMetadata);
  }

  @Override
  public FeesAndCredits customizeCreatePrice(final CreatePriceParameters priceParameters)
      throws EppException {

    if (dpmlLookup.isBlocked(priceParameters.domainName(), priceParameters.asOfDate())) {
      return priceParameters
          .feesAndCredits()
          .asBuilder()
          .addFeeOrCredit(Fee.create(overridePrice.getAmount(), FeeType.DPML))
          .setFeeExtensionRequired(true)
          .build();
    }

    return super.customizeCreatePrice(priceParameters);
  }
}
