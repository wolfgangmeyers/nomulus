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

import com.google.api.services.oauth2.Oauth2;
import com.google.api.services.oauth2.model.Tokeninfo;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * This class handles authorizing an oauth token and validates if the user is able to access the
 * application
 */
public class ServiceAccountAuthenticator {

  private static final Logger logger = Logger.getLogger(ServiceAccountAuthenticator.class.getName());
  private static final String prefix = "Bearer";
  private final String oauthId;
  private final Oauth2 oauth2;
  private final ImmutableSet<String> authorizedServiceAccounts;

  @Inject
  public ServiceAccountAuthenticator
  (@Named("authorizedServiceAccounts") ImmutableSet<String> authorizedServiceAccounts,
      @Named("oauthId") String oauthId,
      Oauth2 oauth2) {
    this.authorizedServiceAccounts = authorizedServiceAccounts;
    this.oauthId = oauthId;
    this.oauth2 = oauth2;
  }

  /**
   * Checks for authorization header. If present runs oauth flows and sets user on session if not
   * return.
   *
   * @param authorization Value of the authorization header
   */
  public boolean authenticate(String authorization) {
    String accessToken = getCredentials(authorization);
    if (accessToken == null) {
      return false;
    }

    Tokeninfo tokeninfo;
    try {
      tokeninfo = oauth2.tokeninfo().setAccessToken(accessToken).execute();
    } catch (IOException e) {
      logger.severe("Error while trying to obtain tokeninfo");
      logger.severe(e.getMessage());
      return false;
    }
    if (!oauthId.equals(tokeninfo.getAudience())) {
      logger.severe("Invalid audience token: " + tokeninfo.getAudience());
      return false;
    }
    if (!authorizedServiceAccounts.contains(tokeninfo.getEmail())) {
      logger.severe("Invalid service account: " + tokeninfo.getEmail());
      return false;
    }
    return true;
  }

  /**
   * Parses a value of the `Authorization` header in the form of `Bearer a892bf3e284da9bb40648ab10`.
   *
   * @param header the value of the `Authorization` header
   * @return a token
   */
  @Nullable
  private String getCredentials(String header) {
    if (header == null) {
      return null;
    }

    final int space = header.indexOf(' ');
    if (space <= 0) {
      return null;
    }

    final String method = header.substring(0, space);
    if (!prefix.equalsIgnoreCase(method)) {
      return null;
    }

    return header.substring(space + 1);
  }
}
