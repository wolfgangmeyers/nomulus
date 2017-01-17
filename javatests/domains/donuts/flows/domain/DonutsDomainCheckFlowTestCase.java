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
