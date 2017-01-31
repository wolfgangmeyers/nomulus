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
