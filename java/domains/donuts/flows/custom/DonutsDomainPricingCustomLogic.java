package domains.donuts.flows.custom;

import static domains.donuts.config.DonutsConfigModule.provideDpmlCreateOverridePrice;
import static domains.donuts.config.DonutsConfigModule.provideDpmlLookup;

import domains.donuts.flows.DpmlLookup;
import google.registry.flows.EppException;
import google.registry.flows.FlowMetadata;
import google.registry.flows.SessionMetadata;
import google.registry.flows.custom.DomainPricingCustomLogic;
import google.registry.flows.domain.FeesAndCredits;
import google.registry.model.domain.fee.BaseFee.FeeType;
import google.registry.model.domain.fee.Fee;
import google.registry.model.eppinput.EppInput;
import org.joda.money.Money;

public class DonutsDomainPricingCustomLogic extends DomainPricingCustomLogic {

  // TODO: Dagger inject these. https://groups.google.com/forum/#!topic/nomulus-discuss/4GkhC9naJmU
  private final DpmlLookup dpmlLookup = provideDpmlLookup();
  private final Money overridePrice = provideDpmlCreateOverridePrice();

  DonutsDomainPricingCustomLogic(
    final EppInput eppInput, final SessionMetadata sessionMetadata, final FlowMetadata flowMetadata) {
    super(eppInput, sessionMetadata, flowMetadata);
  }

  @Override
  public FeesAndCredits customizeCreatePrice(final CreatePriceParameters priceParameters)
      throws EppException {

    if (dpmlLookup.isBlocked(priceParameters.domainName(), priceParameters.asOfDate())) {
      return priceParameters
          .feesAndCredits()
          .asBuilder()
          .addFeeOrCredit(Fee.create(overridePrice.getAmount(), FeeType.DPML))
          .setFeeExtensionRequired(true)
          .build();
    }

    return super.customizeCreatePrice(priceParameters);
  }
}
