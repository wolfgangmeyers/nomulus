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

package domains.donuts.flows.custom;

import static com.google.common.truth.Truth.assertThat;
import static google.registry.testing.DatastoreHelper.createTld;
import static google.registry.testing.DatastoreHelper.persistReservedList;
import static google.registry.testing.DatastoreHelper.persistResource;
import static org.mockito.Mockito.doReturn;

import com.google.common.collect.ImmutableList;
import google.registry.flows.FlowMetadata;
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
  @Mock private FlowMetadata flowMetadata;

  private final DonutsDomainCheckFlowCustomLogic tested =
      new DonutsDomainCheckFlowCustomLogic(eppInput, sessionMetadata, flowMetadata);

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
