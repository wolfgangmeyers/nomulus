package domains.donuts.flows;

import org.joda.time.DateTime;

public interface DpmlLookup {
  /**
   * Checks if the provided label should be marked as a DPML block
   *
   * @param label the sld to look up
   * @param now the time at which to check if the label exists
   * @return true if the label should be blocked
   */
  boolean isBlocked(final String label, final DateTime now);
}
