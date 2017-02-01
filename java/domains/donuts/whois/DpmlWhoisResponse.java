// Copyright 2016 The Nomulus Authors. All Rights Reserved.
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

package domains.donuts.whois;

import google.registry.whois.WhoisResponseImpl;
import org.joda.time.DateTime;

/** Represents a DPML WHOIS response to a domain query. */
public class DpmlWhoisResponse extends WhoisResponseImpl {

  public static final String DPML_WHOIS_RESPONSE =
      "This is not a domain registration. It is a DPML block. " +
      "Additional information can be found at http://www.donuts.co/dpml.";

  public DpmlWhoisResponse(DateTime timestamp) {
    super(timestamp);
  }

  @Override
  public String getPlainTextOutput(boolean preferUnicode, String disclaimer) {
    return new DpmlEmitter()
        .emitRawLine(DPML_WHOIS_RESPONSE)
        .emitNewline()
        .emitFooter(disclaimer)
        .toString();
  }

  static class DpmlEmitter extends Emitter<DpmlEmitter> {}
}
