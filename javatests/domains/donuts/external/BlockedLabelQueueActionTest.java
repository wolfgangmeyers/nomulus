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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import domains.donuts.registry.service.ServiceAccountAuthenticator;
import google.registry.request.HttpException;
import google.registry.testing.AppEngineRule;
import javax.servlet.http.HttpServletRequest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class BlockedLabelQueueActionTest {

  @Rule public final AppEngineRule appEngine = AppEngineRule.builder()
      .withTaskQueue(readResourceUtf8("domains/donuts/env/common/default/WEB-INF/queue.xml"))
      .withDatastore()
      .build();

  @Rule public final ExpectedException thrown = ExpectedException.none();

  @Mock
  private HttpServletRequest request;

  @Mock
  private ServiceAccountAuthenticator authenticator;

  private final BlockedLabelQueueAction tested = new BlockedLabelQueueAction();

  @Before
  public void setUp() throws Exception {
    tested.labels = new String[]{"label"};
    tested.request = request;
    when(tested.request.getHeader("Authorization")).thenReturn("Bearer: xyz");
    tested.authenticator = authenticator;
    when(tested.authenticator.authenticate("Bearer: xyz")).thenReturn(true);
  }

  @Test
  public void testRun() throws Exception {
    tested.action = "create";
    tested.run();
    verify(tested.authenticator).authenticate("Bearer: xyz");
  }

  @Test
  public void testRun_InvalidArgument() throws Exception {
    tested.action = null;
    thrown.expect(HttpException.BadRequestException.class);
    tested.run();
  }

  @Test
  public void testRun_Unauthorized() throws Exception {
    when(tested.request.getHeader("Authorization")).thenReturn("Bearer: abc");
    thrown.expect(HttpException.ForbiddenException.class);
    tested.run();
  }
}
