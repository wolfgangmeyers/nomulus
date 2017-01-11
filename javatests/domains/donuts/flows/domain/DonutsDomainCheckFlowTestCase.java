package domains.donuts.flows.domain;

import static google.registry.model.eppoutput.CheckData.DomainCheck.create;

import domains.donuts.flows.DonutsResourceFlowTestCase;
import google.registry.flows.domain.DomainCheckFlow;
import google.registry.model.domain.DomainResource;
import google.registry.model.eppoutput.CheckData;

public abstract class DonutsDomainCheckFlowTestCase
    extends DonutsResourceFlowTestCase<DomainCheckFlow, DomainResource> {

  protected CheckData runDonutsCheckFlow() throws Exception {
    assertTransactionalFlow(false);
    return (CheckData) super.runDonutsFlow().getResponse().getResponseData().get(0);
  }

  protected CheckData.DomainCheck unavailableCheck(final String tld, final String reason) {
    return create(false, tld, reason);
  }

  protected CheckData.DomainCheck availableCheck(final String tld) {
    return create(true, tld, null);
  }
}
