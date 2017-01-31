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

import static google.registry.util.ResourceUtils.readResourceUtf8;

import google.registry.request.HttpException;
import google.registry.testing.AppEngineRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class BlockedLabelQueueActionTest {

  @Rule public final AppEngineRule appEngine = AppEngineRule.builder()
      .withTaskQueue(readResourceUtf8("domains/donuts/env/common/default/WEB-INF/queue.xml"))
      .withDatastore()
      .build();

  @Rule public final ExpectedException thrown = ExpectedException.none();

  private final BlockedLabelQueueAction tested = new BlockedLabelQueueAction();

  @Before
  public void setUp() throws Exception {
    tested.labels = new String[]{"label"};
  }

  @Test
  public void testRun() throws Exception {
    tested.action = "create";
    tested.run();
  }

  @Test
  public void testRun_InvalidArgument() throws Exception {
    tested.action = null;
    thrown.expect(HttpException.BadRequestException.class);
    tested.run();
  }
}
