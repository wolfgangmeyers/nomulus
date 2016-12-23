package domains.donuts.external;

import domains.donuts.external.BlockedLabelConstants.Action;
import google.registry.model.external.BlockedLabel;
import google.registry.request.Parameter;
import google.registry.util.FormattingLogger;
import org.joda.time.DateTime;

import javax.inject.Inject;
import java.util.Arrays;

import static domains.donuts.external.BlockedLabelConstants.ACTION_PARAM;
import static domains.donuts.external.BlockedLabelConstants.ADDED_TO_QUEUE;
import static domains.donuts.external.BlockedLabelConstants.LABEL_PARAM;
import static google.registry.request.Action.Method.POST;

@google.registry.request.Action(path = "/_ah/queue/queue-blocked-label", method = POST)
public class BlockedLabelWorkerAction implements Runnable {

  private static final FormattingLogger logger = FormattingLogger.getLoggerForCallerClass();

  @Inject @Parameter(LABEL_PARAM) String[] labels;
  @Inject @Parameter(ACTION_PARAM) String actionString;
  @Inject @Parameter(ADDED_TO_QUEUE) DateTime addedToQueue;
  @Inject public BlockedLabelWorkerAction() {}

  @Override
  public void run() {
    final Action action = Action.of(actionString);

    logger.info(String.format("Labels: %s, Action: [%s]", Arrays.toString(labels), action));

    switch (action) {
      case CREATE: BlockedLabel.create(labels, addedToQueue);
        break;
      case DELETE: BlockedLabel.delete(labels, addedToQueue);
        break;
    }
  }
}
