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

package domains.donuts.module.backend;

import static domains.donuts.external.BlockedLabelConstants.ACTION_PARAM;
import static domains.donuts.external.BlockedLabelConstants.ADDED_TO_QUEUE;
import static domains.donuts.external.BlockedLabelConstants.LABEL_PARAM;
import static google.registry.model.registry.Registries.assertTldExists;
import static google.registry.request.RequestParameters.extractOptionalDatetimeParameter;
import static google.registry.request.RequestParameters.extractRequiredParameter;

import com.google.common.base.Optional;
import dagger.Module;
import dagger.Provides;
import google.registry.batch.ExpandRecurringBillingEventsAction;
import google.registry.request.Parameter;
import google.registry.request.RequestParameters;
import javax.servlet.http.HttpServletRequest;
import org.joda.time.DateTime;

/**
 * Dagger module for injecting common settings for all Backend tasks.
 */
@Module
public class BackendModule {

  @Provides
  @Parameter(RequestParameters.PARAM_TLD)
  static String provideTld(HttpServletRequest req) {
    return assertTldExists(extractRequiredParameter(req, RequestParameters.PARAM_TLD));
  }

  @Provides
  @Parameter("cursorTime")
  static Optional<DateTime> provideCursorTime(HttpServletRequest req) {
    return extractOptionalDatetimeParameter(
        req, ExpandRecurringBillingEventsAction.PARAM_CURSOR_TIME);
  }

  @Provides
  @Parameter(LABEL_PARAM)
  static String[] provideLabels(HttpServletRequest req) {
    return req.getParameterValues(LABEL_PARAM);
  }

  @Provides
  @Parameter(ACTION_PARAM)
  static String provideAction(HttpServletRequest req) {
    return req.getParameter(ACTION_PARAM);
  }

  @Provides
  @Parameter(ADDED_TO_QUEUE)
  static DateTime providedAddedToQueue(HttpServletRequest req) {
    return DateTime.parse(req.getParameter(ADDED_TO_QUEUE));
  }
}
