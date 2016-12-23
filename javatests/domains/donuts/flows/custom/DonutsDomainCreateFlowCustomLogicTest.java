package domains.donuts.flows.custom;

import com.google.common.collect.ImmutableList;
import domains.donuts.flows.custom.DonutsDomainCreateFlowCustomLogic.DpmlBlockedException;
import domains.donuts.flows.custom.DonutsDomainCreateFlowCustomLogic.SignedMarksRequiredException;
import google.registry.flows.SessionMetadata;
import google.registry.flows.custom.DomainCreateFlowCustomLogic.AfterValidationParameters;
import google.registry.flows.custom.DomainCreateFlowCustomLogic.BeforeSaveParameters;
import google.registry.model.domain.DomainResource;
import google.registry.model.domain.launch.LaunchCreateExtension;
import google.registry.model.domain.launch.LaunchPhase;
import google.registry.model.eppinput.EppInput;
import google.registry.model.external.BlockedLabel;
import google.registry.model.reporting.HistoryEntry;
import google.registry.model.smd.AbstractSignedMark;
import google.registry.testing.AppEngineRule;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static domains.donuts.config.DonutsConfigModule.provideDpmlTld;
import static google.registry.testing.DatastoreHelper.createTld;
import static google.registry.testing.DatastoreHelper.persistActiveDomain;
import static google.registry.testing.DatastoreHelper.persistResource;
import static org.mockito.Mockito.doReturn;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("ResultOfMethodCallIgnored")
public class DonutsDomainCreateFlowCustomLogicTest {

  @Rule public final AppEngineRule appEngine = AppEngineRule.builder().withDatastore().build();
  @Rule public final ExpectedException thrown = ExpectedException.none();

  @Mock private EppInput eppInput;
  @Mock private SessionMetadata sessionMetadata;
  @Mock private LaunchCreateExtension launchCreate;
  @Mock private AfterValidationParameters afterParameters;
  @Mock private BeforeSaveParameters beforeParameters;
  @Mock private AbstractSignedMark signedMark;
  @Mock private DomainResource domainResource;
  @Mock private HistoryEntry historyEntry;

  private DonutsDomainCreateFlowCustomLogic tested;

  @Before
  public void setUp() throws Exception {
    createTld("tld");
    doReturn(launchCreate).when(eppInput).getSingleExtension(LaunchCreateExtension.class);
    doReturn("sld.tld").when(domainResource).getFullyQualifiedDomainName();
    doReturn(domainResource).when(beforeParameters).newDomain();
    doReturn(historyEntry).when(beforeParameters).historyEntry();
    doReturn(DateTime.now()).when(historyEntry).getModificationTime();
    tested = new DonutsDomainCreateFlowCustomLogic(eppInput, sessionMetadata);
  }

  @Test
  public void testAfterValidation_NotDpmlRegistration() throws Exception {
    // DPML exceptions are not thrown unless the DPML phase was supplied in the LaunchCreateExtension
    doReturn(LaunchPhase.OPEN).when(launchCreate).getPhase();
    tested.afterValidation(afterParameters);
  }

  @Test
  public void testAfterValidation_DpmlRegistration_NoSignedMarks() throws Exception {
    doReturn(LaunchPhase.DPML).when(launchCreate).getPhase();
    doReturn(ImmutableList.of()).when(launchCreate).getSignedMarks();
    // Signed marks are required if the DPML phase was chosen
    thrown.expect(SignedMarksRequiredException.class);
    tested.afterValidation(afterParameters);
  }

  @Test
  public void testAfterValidation_DpmlRegistration_SignedMarks() throws Exception {
    doReturn(LaunchPhase.DPML).when(launchCreate).getPhase();
    doReturn(ImmutableList.of(signedMark)).when(launchCreate).getSignedMarks();
    // The create is valid if DPML is chosen AND a signed mark is supplied
    tested.afterValidation(afterParameters);
  }

  @Test
  public void testBeforeSave_PremiumDomain() throws Exception {
    // Premium pricing is applied before DPML blocks
    doReturn("rich.tld").when(domainResource).getFullyQualifiedDomainName();
    tested.beforeSave(beforeParameters);
  }

  @Test
  public void testBeforeSave_SmdOverride() throws Exception {
    // Override the block by supplying the SMD
    doReturn("smdId").when(domainResource).getSmdId();
    tested.beforeSave(beforeParameters);
  }

  @Test
  public void testBeforeSave_LabelNotInDpmlTld() throws Exception {
    // Ignore the block if the tld is not registered in the DPML tld
    tested.beforeSave(beforeParameters);
  }

  @Test
  public void testBeforeSave_LabelBlockedByDpml() throws Exception {
    // Note: This test will need to be updated once the DPML lookup has been switched to use internal
    persistResource(new BlockedLabel.Builder()
        .setLabel("sld")
        .setDateCreated(DateTime.now())
        .setDateModified(DateTime.now())
        .build());
    // Block the creation of a label if it exists in the DPML tld
    thrown.expect(DpmlBlockedException.class);
    tested.beforeSave(beforeParameters);
  }
}
