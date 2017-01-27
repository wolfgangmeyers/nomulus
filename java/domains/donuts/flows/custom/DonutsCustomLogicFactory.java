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
