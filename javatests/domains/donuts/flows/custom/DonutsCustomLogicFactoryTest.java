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

import google.registry.flows.FlowMetadata;
import google.registry.flows.SessionMetadata;
import google.registry.model.eppinput.EppInput;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DonutsCustomLogicFactoryTest {

  @Mock private EppInput eppInput;
  @Mock private SessionMetadata sessionMetadata;
  @Mock private FlowMetadata flowMetadata;

  private final DonutsCustomLogicFactory tested = new DonutsCustomLogicFactory();

  @Test
  public void testForDomainCreateFlow() throws Exception {
    assertThat(tested.forDomainCreateFlow(eppInput, sessionMetadata, flowMetadata))
        .isInstanceOf(DonutsDomainCreateFlowCustomLogic.class);
  }

  @Test
  public void testForDomainCheckFlow() throws Exception {
    assertThat(tested.forDomainCheckFlow(eppInput, sessionMetadata, flowMetadata))
        .isInstanceOf(DonutsDomainCheckFlowCustomLogic.class);
  }

  @Test
  public void testForDomainPricing() throws Exception {
    assertThat(tested.forDomainPricing(eppInput, sessionMetadata, flowMetadata))
        .isInstanceOf(DonutsDomainPricingCustomLogic.class);
  }
}
