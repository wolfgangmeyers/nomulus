package domains.donuts.flows;

import static google.registry.pricing.PricingEngineProxy.isDomainPremium;

import com.google.common.net.InternetDomainName;
import org.joda.time.DateTime;

public abstract class DpmlLookup {

  /**
   * Checks if the provided label should be marked as a DPML block.
   *
   * A label will only be checked if it is not in the premium
   *
   * @param domainName the domain name to look up
   * @param now the time at which to check if the label exists
   * @return true if the label should be blocked
   */
  public boolean isBlocked(final InternetDomainName domainName, final DateTime now) {
    return !isDomainPremium(domainName.toString(), now)
      && shouldBlock(domainName.parts().get(0), now);
  }

  /**
   * This method needs to be implemented with the logic to check if the label should be blocked.
   *
   * @param label the label to check if blocked by DPML
   * @param now the transaction time
   * @return true if the label should be blocked
   */
  protected abstract boolean shouldBlock(final String label, final DateTime now);
}
