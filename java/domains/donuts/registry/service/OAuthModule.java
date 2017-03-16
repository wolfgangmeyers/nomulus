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

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.oauth2.Oauth2;
import com.google.common.collect.ImmutableSet;
import dagger.Module;
import dagger.Provides;
import java.io.IOException;
import java.util.Arrays;
import javax.inject.Named;

/** Provides oauth service configuration */
@Module
public class OAuthModule {

  static final ImmutableSet<String> AUTHORIZED_SERVICE_ACCOUNTS = ImmutableSet.of(
      "titan-service@mercury-donuts-alpha.iam.gserviceaccount.com"
  );

  @Provides @Named("authorizedServiceAccounts")
  public static ImmutableSet<String> provideAuthorizedServiceAccounts() {
    return AUTHORIZED_SERVICE_ACCOUNTS;
  }

  @Provides @Named("oauthId")
  public static String provideOauthId() {
    // audience id for the titan service account
    return "110674222423300449361";
  }

  @Provides
  public static Oauth2 provideOauth2(HttpTransport httpTransport,
                              JsonFactory jsonFactory,
                              Credential credential) {
    return new Oauth2.Builder(httpTransport, jsonFactory, credential)
               .setApplicationName("Mercury Donuts")
               .build();
  }

  @Provides
  public static Credential provideCredential() {
    try {
      return GoogleCredential.getApplicationDefault()
                 .createScoped(Arrays.asList("https://www.googleapis.com/auth/userinfo.profile",
                     "https://www.googleapis.com/auth/userinfo.email"));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
