package domains.donuts.flows;

import com.google.common.collect.ImmutableList;

import org.joda.time.DateTime;

import java.util.Set;

import google.registry.model.domain.DomainResource;
import google.registry.model.registry.Registry;

import static domains.donuts.config.DonutsConfigModule.provideDpmlTld;
import static google.registry.model.EppResourceUtils.checkResourcesExist;

/** Utilities specific to Donuts DPML functionality */
public final class DonutsFlowUtil {

  private DonutsFlowUtil() {}

  /**
   * Checks if the provided label is registered in the DPML zone
   *
   * @param label the sld to check in the DPML zone
   * @param now the time at which to check if the label exists
   * @return true if the label is registered in the DPML zone
   */
  public static boolean labelExistsInDpml(final String label, final DateTime now) {
    final String fqdn = label + "." + provideDpmlTld();
    final Set<String> existingDomains =
        checkResourcesExist(DomainResource.class, ImmutableList.of(fqdn), now);
    return Registry.exists(provideDpmlTld()) && existingDomains.contains(fqdn);
  }
}
