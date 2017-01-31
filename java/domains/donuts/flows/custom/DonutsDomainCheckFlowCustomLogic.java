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

import static domains.donuts.config.DonutsConfigModule.provideDpmlLookup;
import static google.registry.model.registry.label.ReservationType.UNRESERVED;

import com.google.common.collect.ImmutableList;
import com.google.common.net.InternetDomainName;
import domains.donuts.flows.DpmlLookup;
import google.registry.flows.EppException;
import google.registry.flows.FlowMetadata;
import google.registry.flows.SessionMetadata;
import google.registry.flows.custom.DomainCheckFlowCustomLogic;
import google.registry.model.eppinput.EppInput;
import google.registry.model.eppoutput.CheckData;
import google.registry.model.registry.label.ReservationType;
import google.registry.model.registry.label.ReservedList;

/** Provides Donuts custom domain check logic */
public class DonutsDomainCheckFlowCustomLogic extends DomainCheckFlowCustomLogic {

  // TODO: Dagger inject this. https://groups.google.com/forum/#!topic/nomulus-discuss/4GkhC9naJmU
  private final DpmlLookup dpmlLookup = provideDpmlLookup();

  DonutsDomainCheckFlowCustomLogic(
    final EppInput eppInput, final SessionMetadata sessionMetadata, final FlowMetadata flowMetadata) {
    super(eppInput, sessionMetadata, flowMetadata);
  }

  @Override
  @SuppressWarnings("ResultOfMethodCallIgnored")
  public BeforeResponseReturnData beforeResponse(final BeforeResponseParameters parameters)
      throws EppException {
    final ImmutableList.Builder<CheckData.DomainCheck> updatedChecks =
        new ImmutableList.Builder<>();
    final ImmutableList<CheckData.DomainCheck> existingChecks = parameters.domainChecks();

    // At this point the Google logic has validated all the checks. We need to reevaluate the
    // domains marked as available & unreserved
    for (CheckData.DomainCheck existing : existingChecks) {
      final String name = existing.getName().getValue();
      final InternetDomainName domainName = InternetDomainName.from(name);
      final ReservationType reservationType =
          ReservedList.getReservation(domainName.parts().get(0), domainName.parent().toString());
      if (existing.getName().getAvail()
          && UNRESERVED.equals(reservationType)
          && dpmlLookup.isBlocked(domainName, parameters.asOfDate())) {
        updatedChecks.add(CheckData.DomainCheck.create(false, name, "DPML block"));
      } else {
        updatedChecks.add(existing);
      }
    }

    return BeforeResponseReturnData.newBuilder()
        .setDomainChecks(updatedChecks.build())
        .setResponseExtensions(parameters.responseExtensions())
        .build();
  }
}
