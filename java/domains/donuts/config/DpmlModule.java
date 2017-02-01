package domains.donuts.config;

import domains.donuts.external.ExternalDpmlLookup;
import domains.donuts.flows.DpmlLookup;

import dagger.Module;
import dagger.Provides;
import google.registry.config.RegistryConfig;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;

@Module
public class DpmlModule {
  @Provides
  @RegistryConfig.Config("dpmlLookup")
  public static DpmlLookup provideDpmlLookup() {
    return new ExternalDpmlLookup();
  }

  @Provides
  @RegistryConfig.Config("dpmlCreateOverridePrice")
  public static Money provideDpmlCreateOverridePrice() {
    return Money.of(CurrencyUnit.USD, 25.00);
  }

  /**
   * Returns the DPML TLD name
   *
   * @return String name of the DPML TLD name
   */
  @Provides
  @RegistryConfig.Config("dpmlTld")
  public static String provideDpmlTld() {
    return "dpml.zone";
  }
}
