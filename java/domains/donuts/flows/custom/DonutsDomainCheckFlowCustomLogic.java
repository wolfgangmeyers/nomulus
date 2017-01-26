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
