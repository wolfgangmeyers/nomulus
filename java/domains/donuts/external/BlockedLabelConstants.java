package domains.donuts.external;

public class BlockedLabelConstants {

  public static final String QUEUE_BLOCKED_LABEL = "queue-blocked-label";
  public static final String LABEL_PARAM = "label";
  public static final String ACTION_PARAM = "action";
  public static final String ADDED_TO_QUEUE = "addedToQueue";
  public static final String REQ_PARAM_ERROR_TEMPLATE = "Required parameter [%s] missing";

  public enum Action {
    CREATE,
    DELETE;

    public static Action of(final String input) {
      if (input != null) {
        for (Action action : Action.values()) {
          if (input.equalsIgnoreCase(action.toString())) {
            return action;
          }
        }
      }
      return null;
    }
  }
}
