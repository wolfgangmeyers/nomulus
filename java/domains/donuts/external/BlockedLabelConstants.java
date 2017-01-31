// Copyright 2016 Donuts Inc. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

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
