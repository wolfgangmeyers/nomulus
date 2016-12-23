package domains.donuts.flows.custom;

import com.google.common.net.InternetDomainName;
import domains.donuts.flows.DonutsLaunchCreateWrapper;
import domains.donuts.flows.DpmlLookup;
import google.registry.flows.SessionMetadata;
import google.registry.flows.custom.DomainCreateFlowCustomLogic;
import google.registry.flows.custom.EntityChanges;
import google.registry.model.eppinput.EppInput;
import org.joda.time.DateTime;

import google.registry.flows.EppException;
import google.registry.flows.EppException.ParameterValuePolicyErrorException;
import google.registry.flows.EppException.StatusProhibitsOperationException;
import google.registry.model.domain.DomainResource;
import google.registry.model.domain.launch.LaunchCreateExtension;

import static domains.donuts.config.DonutsConfigModule.provideDpmlLookup;
import static google.registry.pricing.PricingEngineProxy.isDomainPremium;

/** Provides Donuts custom domain create logic */
public class DonutsDomainCreateFlowCustomLogic extends DomainCreateFlowCustomLogic {

  // TODO: Dagger inject this. https://groups.google.com/forum/#!topic/nomulus-discuss/4GkhC9naJmU
  private final DpmlLookup dpmlLookup = provideDpmlLookup();

  private final DonutsLaunchCreateWrapper launchCreateWrapper;

  DonutsDomainCreateFlowCustomLogic(final EppInput eppInput, final SessionMetadata sessionMetadata) {
    super(eppInput, sessionMetadata);
    this.launchCreateWrapper = new DonutsLaunchCreateWrapper(
        eppInput.getSingleExtension(LaunchCreateExtension.class));
  }

  @Override
  public void afterValidation(final AfterValidationParameters parameters) throws EppException {
    if (launchCreateWrapper.isDpmlRegistration()) {
      verifySignedMarkProvided();
    }
  }

  @Override
  public EntityChanges beforeSave(final BeforeSaveParameters parameters) throws EppException {
    verifyDpmlAllows(parameters.newDomain(), parameters.historyEntry().getModificationTime());
    return parameters.entityChanges();
  }

  private void verifyDpmlAllows(final DomainResource domain, final DateTime now) throws DpmlBlockedException {
    final String fqdn = domain.getFullyQualifiedDomainName();

    // Do NOT block labels that exist in both dpml and premium. Premium names need to be bought
    // individually to add coverage.
    if (!isDomainPremium(fqdn, now)
        // The existence of the SmdId means a valid signed mark was supplied. Allow the user to override the current
        // dpml block by not throwing the exception.
        && domain.getSmdId() == null
        && dpmlLookup.isBlocked(InternetDomainName.from(fqdn).parts().get(0), now)) {
      throw new DpmlBlockedException();
    }
  }

  /** Validates signed marks were provided on the current flow */
  private void verifySignedMarkProvided() throws SignedMarksRequiredException {
    // DPML requires a signed mark to create
    if (launchCreateWrapper.getSignedMarks().isEmpty()) {
      throw new SignedMarksRequiredException();
    }
  }

  /** The requested domain name is contained within the DPML tld */
  public static class DpmlBlockedException extends StatusProhibitsOperationException {
    public DpmlBlockedException() {
      super("The requested domain name is blocked by DPML");
    }
  }

  /** A DPML create requires signed marks */
  public static class SignedMarksRequiredException extends ParameterValuePolicyErrorException {
    public SignedMarksRequiredException() {
      super("SMD required for DPML block registration");
    }
  }
}
