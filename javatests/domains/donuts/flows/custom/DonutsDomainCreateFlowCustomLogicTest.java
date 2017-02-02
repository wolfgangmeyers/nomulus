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

import static google.registry.model.ofy.ObjectifyService.ofy;
import static google.registry.testing.DatastoreHelper.createTld;
import static google.registry.testing.DatastoreHelper.persistResource;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.net.InternetDomainName;
import com.googlecode.objectify.VoidWork;
import domains.donuts.flows.custom.DonutsDomainCreateFlowCustomLogic.DpmlBlockedException;
import domains.donuts.flows.custom.DonutsDomainCreateFlowCustomLogic.SignedMarksRequiredException;
import google.registry.flows.EppException;
import google.registry.flows.FlowMetadata;
import google.registry.flows.SessionMetadata;
import google.registry.flows.custom.DomainCreateFlowCustomLogic.AfterValidationParameters;
import google.registry.model.domain.launch.LaunchCreateExtension;
import google.registry.model.domain.launch.LaunchPhase;
import google.registry.model.eppinput.EppInput;
import google.registry.model.external.BlockedLabel;
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

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("ResultOfMethodCallIgnored")
public class DonutsDomainCreateFlowCustomLogicTest {

  @Rule public final AppEngineRule appEngine = AppEngineRule.builder().withDatastore().build();
  @Rule public final ExpectedException thrown = ExpectedException.none();

  @Mock private EppInput eppInput;
  @Mock private SessionMetadata sessionMetadata;
  @Mock private LaunchCreateExtension launchCreate;
  @Mock private AfterValidationParameters afterParameters;
  @Mock private AbstractSignedMark signedMark;
  @Mock private FlowMetadata flowMetadata;

  private DonutsDomainCreateFlowCustomLogic tested;

  @Before
  public void setUp() throws Exception {
    createTld("tld");
    doReturn(Optional.absent()).when(afterParameters).signedMarkId();
    doReturn(launchCreate).when(eppInput).getSingleExtension(LaunchCreateExtension.class);
    tested = spy(new DonutsDomainCreateFlowCustomLogic(eppInput, sessionMetadata, flowMetadata));
  }

  @Test
  public void testAfterValidation_NotDpmlRegistration() throws Exception {
    doNothing().when(tested).verifyDpmlAllows(any(InternetDomainName.class), anyBoolean(), any(DateTime.class));
    // DPML exceptions are not thrown unless the DPML phase was supplied in the LaunchCreateExtension
    doReturn(LaunchPhase.OPEN).when(launchCreate).getPhase();
    ofy().transact(new VoidWork() {
      @Override
      public void vrun() {
        try {
          tested.afterValidation(afterParameters);
        } catch (EppException e) {
          throw new RuntimeException(e.getMessage(), e);
        }
      }
    });
  }

  @Test
  public void testAfterValidation_DpmlRegistration_NoSignedMarks() throws Exception {
    doReturn(LaunchPhase.DPML).when(launchCreate).getPhase();
    doThrow(SignedMarksRequiredException.class).when(tested).verifySignedMarkProvided();
    // Signed marks are required if the DPML phase was chosen
    thrown.expect(SignedMarksRequiredException.class);
    tested.afterValidation(afterParameters);
  }

  @Test
  public void testVerifyDpmlAllows_PremiumDomain() throws Exception {
    tested.verifyDpmlAllows(InternetDomainName.from("rich.tld"), false, DateTime.now());
  }

  @Test
  public void testVerifyDpmlAllows_SignedMarkOverride() throws Exception {
    tested.verifyDpmlAllows(InternetDomainName.from("sld.tld"), true, DateTime.now());
  }

  @Test
  public void testVerifyDpmlAllows_DpmlBlock() throws Exception {
    // Note: This test will need to be updated once the DPML lookup has been switched to use internal
    persistResource(new BlockedLabel.Builder()
        .setLabel("sld")
        .setDateCreated(DateTime.now())
        .setDateModified(DateTime.now())
        .build());
    thrown.expect(DpmlBlockedException.class);
    thrown.expectMessage("The requested domain name is blocked by DPML");
    tested.verifyDpmlAllows(InternetDomainName.from("sld.tld"), false, DateTime.now());
  }

  @Test
  public void testVerifySignedMarkProvided() throws Exception {
    doReturn(ImmutableList.of(signedMark)).when(launchCreate).getSignedMarks();
    tested.verifySignedMarkProvided();
  }

  @Test
  public void testVerifySignedMarkProvided_NoSignedMarks() throws Exception {
    doReturn(ImmutableList.of()).when(launchCreate).getSignedMarks();
    thrown.expect(SignedMarksRequiredException.class);
    thrown.expectMessage("SMD required for DPML block registration");
    tested.verifySignedMarkProvided();
  }

  @Test
  public void testAfterValidation_DpmlBlockSuperuser_ShouldNotThrowException() throws Exception {
    doReturn(true).when(flowMetadata).isSuperuser();
    persistResource(new BlockedLabel.Builder()
      .setLabel("sld")
      .setDateCreated(DateTime.now())
      .setDateModified(DateTime.now())
      .build());
    tested.afterValidation(afterParameters);
  }
}
