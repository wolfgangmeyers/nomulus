package domains.donuts.flows.custom;

import com.google.common.collect.ImmutableList;
import google.registry.flows.SessionMetadata;
import google.registry.flows.custom.DomainCheckFlowCustomLogic.BeforeResponseParameters;
import google.registry.flows.custom.DomainCheckFlowCustomLogic.BeforeResponseReturnData;
import google.registry.model.eppinput.EppInput;
import google.registry.model.eppoutput.CheckData;
import google.registry.model.eppoutput.EppResponse;
import google.registry.model.external.BlockedLabel;
import google.registry.model.registry.Registry;
import google.registry.model.registry.label.ReservedList;
import google.registry.testing.AppEngineRule;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static com.google.common.truth.Truth.assertThat;
import static domains.donuts.config.DonutsConfigModule.provideDpmlTld;
import static google.registry.testing.DatastoreHelper.createTld;
import static google.registry.testing.DatastoreHelper.persistActiveDomain;
import static google.registry.testing.DatastoreHelper.persistReservedList;
import static google.registry.testing.DatastoreHelper.persistResource;
import static org.mockito.Mockito.doReturn;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("ResultOfMethodCallIgnored")
public class DonutsDomainCheckFlowCustomLogicTest {

  @Rule public final AppEngineRule appEngine = AppEngineRule.builder().withDatastore().build();

  @Mock private EppInput eppInput;
  @Mock private SessionMetadata sessionMetadata;
  @Mock private BeforeResponseParameters parameters;
  @Mock private CheckData.DomainCheck domainCheck;
  @Mock private CheckData.CheckName checkName;
  @Mock private EppResponse.ResponseExtension responseExtension;

  private final DonutsDomainCheckFlowCustomLogic tested =
      new DonutsDomainCheckFlowCustomLogic(eppInput, sessionMetadata);

  @Before
  public void setUp() throws Exception {
    createTld("tld");
    doReturn(ImmutableList.of(responseExtension)).when(parameters).responseExtensions();
    doReturn(checkName).when(domainCheck).getName();
    doReturn("sld.tld").when(checkName).getValue();
    doReturn(DateTime.now()).when(parameters).asOfDate();
  }

  @Test
  public void testBeforeResponse_NoChecks() throws Exception {
    // Skip the loop as no domain checks are present
    doReturn(ImmutableList.of()).when(parameters).domainChecks();
    final BeforeResponseReturnData result = tested.beforeResponse(parameters);
    assertThat(result.domainChecks()).isEmpty();
  }

  @Test
  public void testBeforeResponse_NotAvailable() throws Exception {
    doReturn(ImmutableList.of(domainCheck)).when(parameters).domainChecks();
    // Don't assign DPML block as something else already has blocked it
    doReturn(false).when(checkName).getAvail();
    final BeforeResponseReturnData result = tested.beforeResponse(parameters);
    assertThat(result.domainChecks()).contains(domainCheck);
  }

  @Test
  public void testBeforeResponse_NotUnreserved() throws Exception {
    doReturn(ImmutableList.of(domainCheck)).when(parameters).domainChecks();
    doReturn(true).when(checkName).getAvail();
    // Don't assign DPML block as it's already reserved
    final ReservedList reservedList = persistReservedList("tld", "sld,FULLY_BLOCKED");
    final Registry tld = Registry.get("tld").asBuilder().setReservedLists(reservedList).build();
    persistResource(tld);
    final BeforeResponseReturnData result = tested.beforeResponse(parameters);
    assertThat(result.domainChecks()).contains(domainCheck);
  }

  @Test
  public void testBeforeResponse_NotInDpmlTld() throws Exception {
    doReturn(ImmutableList.of(domainCheck)).when(parameters).domainChecks();
    doReturn(true).when(checkName).getAvail();
    final ReservedList reservedList = persistReservedList("tld", "sld,UNRESERVED");
    final Registry tld = Registry.get("tld").asBuilder().setReservedLists(reservedList).build();
    persistResource(tld);
    // Don't assign DPML block as it doesn't exist in the DPML tld
    final BeforeResponseReturnData result = tested.beforeResponse(parameters);
    assertThat(result.domainChecks()).contains(domainCheck);
  }

  @Test
  public void testBeforeResponse_external_DpmlBlocked() throws Exception {
    // Note: This test will need to be updated once the DPML lookup has been switched to use internal
    doReturn(ImmutableList.of(domainCheck)).when(parameters).domainChecks();
    doReturn(true).when(checkName).getAvail();
    final ReservedList reservedList = persistReservedList("tld", "sld,UNRESERVED");
    final Registry tld = Registry.get("tld").asBuilder().setReservedLists(reservedList).build();
    persistResource(tld);
    persistResource(new BlockedLabel.Builder()
        .setLabel("sld")
        .setDateCreated(DateTime.now())
        .setDateModified(DateTime.now())
        .build());
    final BeforeResponseReturnData result = tested.beforeResponse(parameters);
    assertThat(result.domainChecks()).hasSize(1);
    assertThat(result.domainChecks().get(0).getReason()).isEqualTo("DPML block");
  }
}
