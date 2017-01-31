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

import google.registry.flows.FlowMetadata;
import google.registry.flows.SessionMetadata;
import google.registry.flows.custom.CustomLogicFactory;
import google.registry.flows.custom.DomainCheckFlowCustomLogic;
import google.registry.flows.custom.DomainCreateFlowCustomLogic;
import google.registry.flows.custom.DomainPricingCustomLogic;
import google.registry.model.eppinput.EppInput;

/**
 * Provides Donuts custom flow logic
 *
 * {@inheritDoc}
 */
public class DonutsCustomLogicFactory extends CustomLogicFactory {

    @Override
    public DomainCreateFlowCustomLogic forDomainCreateFlow(
      EppInput eppInput, SessionMetadata sessionMetadata, FlowMetadata flowMetadata) {
        return new DonutsDomainCreateFlowCustomLogic(eppInput, sessionMetadata, flowMetadata);
    }

    @Override
    public DomainCheckFlowCustomLogic forDomainCheckFlow(
      EppInput eppInput, SessionMetadata sessionMetadata, FlowMetadata flowMetadata) {
        return new DonutsDomainCheckFlowCustomLogic(eppInput, sessionMetadata, flowMetadata);
    }

    @Override
    public DomainPricingCustomLogic forDomainPricing(
      EppInput eppInput, SessionMetadata sessionMetadata, FlowMetadata flowMetadata) {
        return new DonutsDomainPricingCustomLogic(eppInput, sessionMetadata, flowMetadata);
    }
}
