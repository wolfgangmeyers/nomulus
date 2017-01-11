package domains.donuts.external;

import static com.google.common.truth.Truth.assertThat;
import static domains.donuts.external.BlockedLabelConstants.Action.CREATE;
import static domains.donuts.external.BlockedLabelConstants.Action.DELETE;
import static google.registry.model.ofy.ObjectifyService.ofy;
import static google.registry.testing.DatastoreHelper.persistResource;

import com.googlecode.objectify.Key;
import google.registry.model.external.BlockedLabel;
import google.registry.testing.AppEngineRule;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class BlockedLabelWorkerActionTest {

  @Rule public final AppEngineRule appEngine = AppEngineRule.builder().withDatastore().build();

  private final String label1 = "label1";
  private final String label2 = "label2";
  private final BlockedLabelWorkerAction tested = new BlockedLabelWorkerAction();

  private Key<BlockedLabel> key1;
  private Key<BlockedLabel> key2;

  @Before
  public void setUp() throws Exception {
    tested.labels = new String[] {label1, label2};
    tested.addedToQueue = DateTime.now();

    key1 = Key.create(BlockedLabel.class, label1);
    key2 = Key.create(BlockedLabel.class, label2);
  }

  @Test
  public void testCreate() throws Exception {
    tested.actionString = CREATE.toString();

    tested.run();

    final BlockedLabel blockedLabel1 = ofy().load().key(key1).now();
    final BlockedLabel blockedLabel2 = ofy().load().key(key2).now();

    assertThat(blockedLabel1).isNotNull();
    assertThat(blockedLabel1.getLabel()).isEqualTo(label1);
    assertThat(blockedLabel2).isNotNull();
    assertThat(blockedLabel2.getLabel()).isEqualTo(label2);
  }

  @Test
  public void testDelete() throws Exception {
    final DateTime created = DateTime.now().minusDays(1);
    persistResource(new BlockedLabel.Builder()
        .setLabel(label1)
        .setDateCreated(created)
        .setDateModified(created.minusDays(1))
        .build());
    persistResource(new BlockedLabel.Builder()
        .setLabel(label2)
        .setDateCreated(created)
        .setDateModified(created.minusDays(1))
        .build());

    tested.actionString = DELETE.toString();

    tested.run();

    final BlockedLabel result1 = ofy().load().key(key1).now();
    assertThat(result1.isDeleted()).isTrue();
    final BlockedLabel result2 = ofy().load().key(key2).now();
    assertThat(result2.isDeleted()).isTrue();
  }

  @Test
  public void testDeleteDoesNotExist() throws Exception {
    // Verify we can delete a non-existent value without throwing errors
    tested.actionString = DELETE.toString();
    tested.run();
  }
}
