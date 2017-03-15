// Copyright 2017 Donuts Inc. All Rights Reserved.
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

package domains.donuts.registry.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import com.google.api.services.oauth2.Oauth2;
import com.google.api.services.oauth2.model.Tokeninfo;
import java.io.IOException;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ServiceAccountAuthenticatorTest {
  @Mock
  private Oauth2 oauth2;
  private String oauthId = "oauthId";
  @Mock
  private Oauth2.Tokeninfo oauth2TokenInfo;

  private ServiceAccountAuthenticator service;

  @Before
  public void before() {
    service = new ServiceAccountAuthenticator(oauthId, oauth2);
  }

  @Test
  public void testAuthenticate() throws Exception {
    Tokeninfo tokeninfo = new Tokeninfo();
    tokeninfo.setAudience(oauthId);
    tokeninfo.setEmail("titan-service@mercury-donuts-alpha.iam.gserviceaccount.com");
    when(oauth2.tokeninfo()).thenReturn(oauth2TokenInfo);
    when(oauth2TokenInfo.setAccessToken("accessToken")).thenReturn(oauth2TokenInfo);
    when(oauth2TokenInfo.execute()).thenReturn(tokeninfo);
    assertTrue(service.authenticate("Bearer accessToken"));
  }

  @Test
  public void testAuthenticateNoAuthorizationHeader() throws Exception {
    Tokeninfo tokeninfo = new Tokeninfo();
    tokeninfo.setAudience(oauthId);
    tokeninfo.setEmail("titan-service@mercury-donuts-alpha.iam.gserviceaccount.com");
    when(oauth2.tokeninfo()).thenReturn(oauth2TokenInfo);
    when(oauth2TokenInfo.setAccessToken("accessToken")).thenReturn(oauth2TokenInfo);
    when(oauth2TokenInfo.execute()).thenReturn(tokeninfo);
    assertFalse(service.authenticate(null));
  }

  @Test
  public void testAuthenticateEmptyAuthorizationHeader() throws Exception {
    Tokeninfo tokeninfo = new Tokeninfo();
    tokeninfo.setAudience(oauthId);
    tokeninfo.setEmail("titan-service@mercury-donuts-alpha.iam.gserviceaccount.com");
    when(oauth2.tokeninfo()).thenReturn(oauth2TokenInfo);
    when(oauth2TokenInfo.setAccessToken("accessToken")).thenReturn(oauth2TokenInfo);
    when(oauth2TokenInfo.execute()).thenReturn(tokeninfo);
    assertFalse(service.authenticate(""));
  }

  @Test
  public void testAuthenticateInvalidAuthorizationHeader() throws Exception {
    Tokeninfo tokeninfo = new Tokeninfo();
    tokeninfo.setAudience(oauthId);
    tokeninfo.setEmail("titan-service@mercury-donuts-alpha.iam.gserviceaccount.com");
    when(oauth2.tokeninfo()).thenReturn(oauth2TokenInfo);
    when(oauth2TokenInfo.setAccessToken("accessToken")).thenReturn(oauth2TokenInfo);
    when(oauth2TokenInfo.execute()).thenReturn(tokeninfo);
    assertFalse(service.authenticate("abcdefg"));
  }

  @Test
  public void testAuthenticateWrongAuthorizationType() throws Exception {
    Tokeninfo tokeninfo = new Tokeninfo();
    tokeninfo.setAudience(oauthId);
    tokeninfo.setEmail("titan-service@mercury-donuts-alpha.iam.gserviceaccount.com");
    when(oauth2.tokeninfo()).thenReturn(oauth2TokenInfo);
    when(oauth2TokenInfo.setAccessToken("accessToken")).thenReturn(oauth2TokenInfo);
    when(oauth2TokenInfo.execute()).thenReturn(tokeninfo);
    assertFalse(service.authenticate("Authorization: Basic QWxhZGRpbjpPcGVuU2VzYW1l"));
  }

  @Test
  public void testAuthenticateOAuthError() throws Exception {
    Tokeninfo tokeninfo = new Tokeninfo();
    tokeninfo.setAudience(oauthId);
    tokeninfo.setEmail("titan-service@mercury-donuts-alpha.iam.gserviceaccount.com");
    when(oauth2.tokeninfo()).thenReturn(oauth2TokenInfo);
    when(oauth2TokenInfo.setAccessToken("accessToken")).thenReturn(oauth2TokenInfo);
    when(oauth2TokenInfo.execute()).thenThrow(new IOException("test exception message"));
    assertFalse(service.authenticate("Bearer accessToken"));
  }

  @Test
  public void testAuthenticateBadAudience() throws Exception {
    Tokeninfo tokeninfo = new Tokeninfo();
    tokeninfo.setAudience("nope");
    tokeninfo.setEmail("titan-service@mercury-donuts-alpha.iam.gserviceaccount.com");
    when(oauth2.tokeninfo()).thenReturn(oauth2TokenInfo);
    when(oauth2TokenInfo.setAccessToken("accessToken")).thenReturn(oauth2TokenInfo);
    when(oauth2TokenInfo.execute()).thenReturn(tokeninfo);
    assertFalse(service.authenticate("Bearer accessToken"));
  }

  @Test
  public void testAuthenticateUnauthorizedUser() throws Exception {
    Tokeninfo tokeninfo = new Tokeninfo();
    tokeninfo.setAudience(oauthId);
    tokeninfo.setEmail("bad-user@foobar.com");
    when(oauth2.tokeninfo()).thenReturn(oauth2TokenInfo);
    when(oauth2TokenInfo.setAccessToken("accessToken")).thenReturn(oauth2TokenInfo);
    when(oauth2TokenInfo.execute()).thenReturn(tokeninfo);
    assertFalse(service.authenticate("Bearer accessToken"));
  }
}
