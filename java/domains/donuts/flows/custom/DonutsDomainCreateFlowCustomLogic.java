package domains.donuts.flows.custom;

import static domains.donuts.config.DonutsConfigModule.provideDpmlLookup;
import static google.registry.model.ofy.ObjectifyService.ofy;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.net.InternetDomainName;
import domains.donuts.flows.DonutsLaunchCreateWrapper;
import domains.donuts.flows.DpmlLookup;
import google.registry.flows.EppException;
import google.registry.flows.EppException.ParameterValuePolicyErrorException;
import google.registry.flows.EppException.StatusProhibitsOperationException;
import google.registry.flows.FlowMetadata;
import google.registry.flows.SessionMetadata;
import google.registry.flows.custom.DomainCreateFlowCustomLogic;
import google.registry.model.domain.launch.LaunchCreateExtension;
import google.registry.model.eppinput.EppInput;
import org.joda.time.DateTime;

/** Provides Donuts custom domain create logic */
public class DonutsDomainCreateFlowCustomLogic extends DomainCreateFlowCustomLogic {

  // TODO: Dagger inject this. https://groups.google.com/forum/#!topic/nomulus-discuss/4GkhC9naJmU
  private final DpmlLookup dpmlLookup = provideDpmlLookup();

  private final DonutsLaunchCreateWrapper launchCreateWrapper;

  DonutsDomainCreateFlowCustomLogic(
    final EppInput eppInput, final SessionMetadata sessionMetadata, final FlowMetadata flowMetadata) {
    super(eppInput, sessionMetadata, flowMetadata);
    this.launchCreateWrapper =
        new DonutsLaunchCreateWrapper(eppInput.getSingleExtension(LaunchCreateExtension.class));
  }

  @Override
  public void afterValidation(final AfterValidationParameters parameters) throws EppException {
    if (launchCreateWrapper.isDpmlRegistration()) {
      verifySignedMarkProvided();
    }

    verifyDpmlAllows(
        parameters.domainName(), parameters.signedMarkId().isPresent(), ofy().getTransactionTime());
  }

  @VisibleForTesting
  void verifyDpmlAllows(
      final InternetDomainName domainName, boolean smdIdPresent, final DateTime now)
      throws DpmlBlockedException {

    // This assumes the DomainCreateFlow has checked the domain name is not reserved and
    // the SMD has already been checked against the domain name.
    if (dpmlLookup.isBlocked(domainName, now) && !smdIdPresent) {
      throw new DpmlBlockedException();
    }
  }

  /** Validates signed marks were provided on the current flow */
  @VisibleForTesting
  void verifySignedMarkProvided() throws SignedMarksRequiredException {
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
