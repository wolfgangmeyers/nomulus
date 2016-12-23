package google.registry.model.external;

import google.registry.testing.AppEngineRule;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static com.google.common.truth.Truth.assertThat;
import static google.registry.model.ofy.ObjectifyService.ofy;
import static google.registry.testing.DatastoreHelper.persistResource;
import static google.registry.util.DateTimeUtils.START_OF_TIME;

public class BlockedLabelTest {

  @Rule public final AppEngineRule appEngine = AppEngineRule.builder()
      .withDatastore()
      .build();

  @Rule public final ExpectedException thrown = ExpectedException.none();

  private final String label = "label1";
  private final String[] labels = new String[] {label};

  @Test
  public void testCreate_New() throws Exception {
    final DateTime time = DateTime.now(DateTimeZone.UTC);
    BlockedLabel.create(labels, time);

    final BlockedLabel result = load(label);
    assertThat(result.getDateCreated()).isEqualTo(time);
    assertThat(result.getDateModified()).isEqualTo(time);
    assertThat(result.isDeleted()).isFalse();
  }

  @Test
  public void testCreate_DeletedInPast() throws Exception {
    final DateTime time = DateTime.now(DateTimeZone.UTC);

    persistResource(new BlockedLabel.Builder()
        .setLabel(label)
        .setDateCreated(time)
        .setDateModified(time.minusDays(1))
        .setDeleted(true)
        .build());

    BlockedLabel.create(labels, time);
    final BlockedLabel result = load(label);
    assertThat(result.getDateCreated()).isEqualTo(time);
    assertThat(result.getDateModified()).isEqualTo(time);
    assertThat(result.isDeleted()).isFalse();
  }

  @Test
  public void testCreate_DeletedInFuture() throws Exception {
    final DateTime time = DateTime.now(DateTimeZone.UTC);

    persistResource(new BlockedLabel.Builder()
        .setLabel(label)
        .setDateCreated(time)
        .setDateModified(time.plusDays(1))
        .setDeleted(true)
        .build());

    BlockedLabel.create(labels, time);
    final BlockedLabel result = load(label);
    assertThat(result.getDateCreated()).isEqualTo(time);
    assertThat(result.getDateModified()).isEqualTo(time.plusDays(1));
    assertThat(result.isDeleted()).isTrue();
  }

  @Test
  public void testDelete_New() throws Exception {
    final DateTime time = DateTime.now(DateTimeZone.UTC);
    BlockedLabel.delete(labels, time);

    final BlockedLabel result = load(label);
    assertThat(result.getDateCreated()).isEqualTo(time);
    assertThat(result.getDateModified()).isEqualTo(time);
    assertThat(result.isDeleted()).isTrue();
  }

  @Test
  public void testDelete_CreatedInPast() throws Exception {
    final DateTime time = DateTime.now(DateTimeZone.UTC);

    persistResource(new BlockedLabel.Builder()
        .setLabel(label)
        .setDateCreated(time.minusDays(1))
        .setDateModified(time.minusDays(1))
        .build());

    BlockedLabel.delete(labels, time);
    final BlockedLabel result = load(label);
    assertThat(result.getDateCreated()).isEqualTo(time.minusDays(1));
    assertThat(result.getDateModified()).isEqualTo(time);
    assertThat(result.isDeleted()).isTrue();
  }

  @Test
  public void testDelete_CreatedInFuture() throws Exception {
    final DateTime time = DateTime.now(DateTimeZone.UTC);

    persistResource(new BlockedLabel.Builder()
        .setLabel(label)
        .setDateCreated(time)
        .setDateModified(time.plusDays(1))
        .setDeleted(false)
        .build());

    BlockedLabel.delete(labels, time);
    final BlockedLabel result = load(label);
    assertThat(result.getDateCreated()).isEqualTo(time);
    assertThat(result.getDateModified()).isEqualTo(time.plusDays(1));
    assertThat(result.isDeleted()).isFalse();
  }

  @Test
  public void testCreateWithoutLabel() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Label must not be null");
    new BlockedLabel.Builder().build();
  }

  @Test
  public void testCreateWithoutDateCreated() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Date created must not be null");
    new BlockedLabel.Builder().setLabel(label).build();
  }

  @Test
  public void testCreateWithoutDateModified() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Date modified must not be null");
    new BlockedLabel.Builder().setLabel(label).setDateCreated(DateTime.now()).build();
  }

  private BlockedLabel load(final String label) {
    return ofy().load().type(BlockedLabel.class).id(label).now();
  }
}
