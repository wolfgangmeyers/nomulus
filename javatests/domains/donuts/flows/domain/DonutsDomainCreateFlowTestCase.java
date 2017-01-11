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
  protected void persistContactsAndHosts(String hostTld) {
    for (int i = 1; i <= 14; ++i) {
      persistActiveHost(String.format("ns%d.example.%s", i, hostTld));
    }
    persistActiveContact("jd1234");
    persistActiveContact("sh8013");
    clock.advanceOneMilli();
  }
}
