package domains.donuts.external;

import com.google.common.base.Optional;
import domains.donuts.flows.DpmlLookup;
import google.registry.model.external.BlockedLabel;
import org.joda.time.DateTime;

public class ExternalDpmlLookup extends DpmlLookup {

  @Override
  protected boolean shouldBlock(final String label, final DateTime now) {
    final Optional<BlockedLabel> blockedLabel = BlockedLabel.get(label);
    return blockedLabel.isPresent() && !blockedLabel.get().isDeleted();
  }
}
