package domains.donuts.external;

import com.google.common.base.Optional;
import domains.donuts.flows.DpmlLookup;
import google.registry.model.external.BlockedLabel;
import org.joda.time.DateTime;

public class ExternalDpmlLookup implements DpmlLookup {
  @Override
  public boolean isBlocked(String label, DateTime now) {
    final Optional<BlockedLabel> blockedLabel = BlockedLabel.get(label);
    return blockedLabel.isPresent() && !blockedLabel.get().isDeleted();
  }
}
