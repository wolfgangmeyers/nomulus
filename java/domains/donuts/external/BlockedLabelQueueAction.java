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

import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.appengine.repackaged.org.joda.time.DateTime;
import com.google.appengine.repackaged.org.joda.time.DateTimeZone;
import domains.donuts.external.BlockedLabelConstants.Action;
import google.registry.request.HttpException.BadRequestException;
import google.registry.request.HttpException.InternalServerErrorException;
import google.registry.request.Parameter;
import google.registry.util.FormattingLogger;

import javax.inject.Inject;
import java.util.Arrays;

import static com.google.appengine.api.taskqueue.TaskOptions.Builder.withParam;
import static com.google.common.base.Preconditions.checkArgument;
import static domains.donuts.external.BlockedLabelConstants.ACTION_PARAM;
import static domains.donuts.external.BlockedLabelConstants.ADDED_TO_QUEUE;
import static domains.donuts.external.BlockedLabelConstants.LABEL_PARAM;
import static domains.donuts.external.BlockedLabelConstants.QUEUE_BLOCKED_LABEL;
import static domains.donuts.external.BlockedLabelConstants.REQ_PARAM_ERROR_TEMPLATE;
import static google.registry.request.Action.Method.POST;

@google.registry.request.Action(path = "/_dr/task/queueBlockedLabel", method = POST)
public class BlockedLabelQueueAction implements Runnable {

  private static final FormattingLogger logger = FormattingLogger.getLoggerForCallerClass();

  private static final Queue queue = QueueFactory.getQueue(QUEUE_BLOCKED_LABEL);

  @Inject @Parameter(LABEL_PARAM) String[] labels;
  @Inject @Parameter(ACTION_PARAM) String action;
  @Inject public BlockedLabelQueueAction() {}

  @Override
  public void run() {
    try {
      // Validate the parameters provided
      checkArgument(labels != null && labels.length > 0, REQ_PARAM_ERROR_TEMPLATE, LABEL_PARAM);
      checkArgument(action != null, REQ_PARAM_ERROR_TEMPLATE, ACTION_PARAM);
      checkArgument(
          Action.of(action) != null,
          "Action [%s] unknown, expected one of %s",
          action,
          Arrays.toString(Action.values()));

      // Build the request to populate the queue
      TaskOptions options = withParam(ACTION_PARAM, action)
          .param(ADDED_TO_QUEUE, DateTime.now(DateTimeZone.UTC).toString());

      for (String label : labels) {
        options = options.param(LABEL_PARAM, label);
      }

      // Add the request to the queue
      queue.add(options);
    } catch (IllegalArgumentException e) {
      throw new BadRequestException(e.getMessage());
    } catch (Exception e) {
      logger.severe(e, String.format("Unknown exception of type [%s]", e.getClass().getCanonicalName()));
      throw new InternalServerErrorException(e.getMessage());
    }
  }
}
