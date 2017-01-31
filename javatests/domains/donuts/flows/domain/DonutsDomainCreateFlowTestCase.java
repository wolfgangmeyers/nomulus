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

import static google.registry.testing.DatastoreHelper.persistActiveContact;
import static google.registry.testing.DatastoreHelper.persistActiveHost;

import domains.donuts.flows.DonutsResourceFlowTestCase;
import google.registry.flows.domain.DomainCreateFlow;
import google.registry.model.domain.DomainResource;
import google.registry.model.eppoutput.EppOutput;

public abstract class DonutsDomainCreateFlowTestCase
    extends DonutsResourceFlowTestCase<DomainCreateFlow, DomainResource> {

  @Override
  protected EppOutput runDonutsFlow() throws Exception {
    assertTransactionalFlow(true);
    return super.runDonutsFlow();
  }

  protected void persistNetContactsAndHosts() {
    persistContactsAndHosts("net");
  }

  /**
   * Create host and contact entries for testing.
   * @param hostTld the TLD of the host (which might be an external TLD)
   */
  protected void persistContactsAndHosts(final String hostTld) {
    for (int i = 1; i <= 14; ++i) {
      persistActiveHost(String.format("ns%d.example.%s", i, hostTld));
    }
    persistActiveContact("jd1234");
    persistActiveContact("sh8013");
    clock.advanceOneMilli();
  }
}
