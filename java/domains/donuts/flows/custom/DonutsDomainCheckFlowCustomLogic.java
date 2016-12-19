package domains.donuts.flows.custom;

import com.google.common.collect.ImmutableList;
import com.google.common.net.InternetDomainName;
import google.registry.flows.EppException;
import google.registry.flows.SessionMetadata;
import google.registry.flows.custom.DomainCheckFlowCustomLogic;
import google.registry.model.eppinput.EppInput;
import google.registry.model.eppoutput.CheckData;
import google.registry.model.registry.label.ReservationType;
import google.registry.model.registry.label.ReservedList;

import static domains.donuts.flows.DonutsFlowUtil.labelExistsInDpml;
import static google.registry.model.registry.label.ReservationType.UNRESERVED;

/** Provides Donuts custom domain check logic */
public class DonutsDomainCheckFlowCustomLogic extends DomainCheckFlowCustomLogic {

    DonutsDomainCheckFlowCustomLogic(final EppInput eppInput, final SessionMetadata sessionMetadata) {
        super(eppInput, sessionMetadata);
    }

    @Override
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public BeforeResponseReturnData beforeResponse(final BeforeResponseParameters parameters) throws EppException {
        final ImmutableList.Builder<CheckData.DomainCheck> updatedChecks = new ImmutableList.Builder<>();
        final ImmutableList<CheckData.DomainCheck> existingChecks = parameters.domainChecks();

        // At this point the Google logic has validated all the checks. We need to reevaluate the
        // domains marked as available & unreserved
        for (CheckData.DomainCheck existing : existingChecks) {
            final String name = existing.getName().getValue();
            final InternetDomainName fqdn = InternetDomainName.from(name);
            final String label = fqdn.parts().get(0);
            final ReservationType reservationType = ReservedList.getReservation(label, fqdn.parent().toString());
            if (existing.getName().getAvail()
                    && UNRESERVED.equals(reservationType)
                    && labelExistsInDpml(label, parameters.asOfDate()) ) {
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
