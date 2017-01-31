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

package domains.donuts.flows.domain;

import static google.registry.model.eppoutput.CheckData.DomainCheck.create;
import static google.registry.model.ofy.ObjectifyService.ofy;

import com.googlecode.objectify.Work;
import domains.donuts.flows.DonutsResourceFlowTestCase;
import google.registry.flows.domain.DomainCheckFlow;
import google.registry.model.domain.DomainResource;
import google.registry.model.eppoutput.CheckData;
import google.registry.model.eppoutput.EppResponse;

public abstract class DonutsDomainCheckFlowTestCase
    extends DonutsResourceFlowTestCase<DomainCheckFlow, DomainResource> {

  protected EppResponse runDonutsCheckFlow() throws Exception {
    assertTransactionalFlow(false);
    return super.runDonutsFlow().getResponse();
  }

  protected CheckData.DomainCheck unavailableCheck(final String tld, final String reason) {
    return create(false, tld, reason);
  }

  protected CheckData.DomainCheck availableCheck(final String tld) {
    return create(true, tld, null);
  }

  protected CheckData runCheckFlow() {
    return ofy().transact(new Work<CheckData>() {
      @Override
      public CheckData run() {
        try {
          return (CheckData) runDonutsCheckFlow().getResponseData().get(0);
        } catch (Exception e) {
          throw new RuntimeException(e.getMessage(), e);
        }
      }
    });
  }
}
