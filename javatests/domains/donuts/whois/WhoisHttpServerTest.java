package domains.donuts.whois;

import static com.google.common.net.MediaType.PLAIN_TEXT_UTF_8;
import static com.google.common.truth.Truth.assertThat;
import static google.registry.testing.DatastoreHelper.createTld;
import static google.registry.testing.DatastoreHelper.persistActiveDomain;
import static google.registry.testing.DatastoreHelper.persistResource;

import google.registry.model.external.BlockedLabel;
import google.registry.model.ofy.Ofy;
import google.registry.testing.AppEngineRule;
import google.registry.testing.FakeClock;
import google.registry.testing.FakeResponse;
import google.registry.testing.InjectRule;
import google.registry.whois.WhoisHttpServer;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

public class WhoisHttpServerTest {

  @Rule public final AppEngineRule appEngine = AppEngineRule.builder()
    .withDatastore()
    .build();

  @Rule
  public final InjectRule inject = new InjectRule();

  private final FakeResponse response = new FakeResponse();
  private final FakeClock clock = new FakeClock(DateTime.parse("2009-06-29T20:13:00Z"));

  private WhoisHttpServer newWhoisHttpServer(String pathInfo) throws Exception {
    Constructor<WhoisHttpServer> constructor = WhoisHttpServer.class.getDeclaredConstructor();
    constructor.setAccessible(true);
    WhoisHttpServer result = constructor.newInstance();
    setPrivateField(result, "clock", clock);
    setPrivateField(result, "expires", Duration.standardHours(1));
    setPrivateField(result, "requestPath", WhoisHttpServer.PATH + pathInfo);
    setPrivateField(result, "response", response);
    setPrivateField(result, "disclaimer", "Doodle Disclaimer");
    setPrivateField(result, "commandFactory", new DonutsWhoisCommandFactory());
    return result;
  }

  private void setPrivateField(Object o, String fieldName, Object value) throws Exception {
    final Field field = o.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(o, value);
  }

  @Before
  public void setUp() throws Exception {
    createTld("lol");
    inject.setStaticField(Ofy.class, "clock", clock);
  }

  @Test
  public void testRun_DpmlBlocked() throws Exception {
    persistResource(
      new BlockedLabel.Builder()
        .setLabel("dpml")
        .setDateCreated(DateTime.now())
        .setDateModified(DateTime.now())
        .build());
    newWhoisHttpServer("dpml.lol").run();
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getPayload()).startsWith(DpmlWhoisResponse.DPML_WHOIS_RESPONSE);
  }

  @Test
  public void testRun_NonDpmlBlocked() throws Exception {
    persistActiveDomain("test.lol");
    persistResource(
      new BlockedLabel.Builder()
        .setLabel("dpml")
        .setDateCreated(DateTime.now())
        .setDateModified(DateTime.now())
        .build());
    newWhoisHttpServer("test.lol").run();
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getPayload()).startsWith("Domain Name: test.lol");
    assertThat(response.getPayload()).doesNotContain(DpmlWhoisResponse.DPML_WHOIS_RESPONSE);
  }

  @Test
  public void testRun_domainNotFound_returns404StatusAndPlainTextResponse() throws Exception {
    newWhoisHttpServer("/domain/cat.lol").run();
    assertThat(response.getStatus()).isEqualTo(404);
    assertThat(response.getContentType()).isEqualTo(PLAIN_TEXT_UTF_8);
  }
}
