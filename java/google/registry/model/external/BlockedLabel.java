package google.registry.model.external;

import com.google.common.base.Optional;
import com.googlecode.objectify.VoidWork;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import google.registry.model.Buildable;
import google.registry.model.ImmutableObject;
import org.joda.time.DateTime;

import static com.google.common.base.Preconditions.checkArgument;
import static google.registry.model.ofy.ObjectifyService.ofy;
import static google.registry.util.DateTimeUtils.START_OF_TIME;

@Entity
public class BlockedLabel extends ImmutableObject implements Buildable, Comparable<BlockedLabel> {

  @Id String label;
  DateTime dateCreated;
  DateTime dateModified;
  boolean deleted;

  public String getLabel() {
    return label;
  }

  public DateTime getDateCreated() {
    return dateCreated;
  }

  public DateTime getDateModified() {
    return dateModified;
  }

  public boolean isDeleted() {
    return deleted;
  }

  public static Optional<BlockedLabel> get(final String label) {
    return Optional.fromNullable(ofy()
        .load()
        .type(BlockedLabel.class)
        .id(label)
        .now());
  }

  public static void create(final String[] labels, final DateTime requestTime) {
    update(labels, requestTime, false);
  }

  public static void delete(final String[] labels, final DateTime requestTime) {
    update(labels, requestTime, true);
  }

  private static void update(final String[] labels, final DateTime requestTime, boolean deleted) {
    for (final String label : labels) {
      final BlockedLabel blockedLabel = get(label).or(new BlockedLabel());

      BlockedLabel.Builder builder = null;
      if (blockedLabel.getDateCreated() == null) {
        builder = blockedLabel.asBuilder()
            .setLabel(label)
            .setDateCreated(requestTime)
            .setDateModified(requestTime)
            .setDeleted(deleted);
      } else if (blockedLabel.getDateModified().isBefore(requestTime)) {
        builder = blockedLabel.asBuilder()
            .setDateModified(requestTime)
            .setDeleted(deleted);
      }

      if (builder != null) {
        final BlockedLabel updated = builder.build();
        ofy().transact(new VoidWork() {
          @Override
          public void vrun() {
            ofy().save().entity(updated).now();
          }
        });
      }
    }
  }

  @Override
  public Builder asBuilder() {
    return new Builder(clone(this));
  }

  @Override
  public int compareTo(BlockedLabel o) {
    return o.getLabel().compareTo(this.getLabel());
  }

  public static class Builder extends Buildable.Builder<BlockedLabel> {
    public Builder() {}

    public Builder(final BlockedLabel instance) {
      super(instance);
    }

    public Builder setLabel(final String label) {
      getInstance().label = label;
      return this;
    }

    public Builder setDateCreated(final DateTime dateCreated) {
      getInstance().dateCreated = dateCreated;
      return this;
    }

    public Builder setDateModified(final DateTime dateModified) {
      getInstance().dateModified = dateModified;
      return this;
    }

    public Builder setDeleted(boolean deleted) {
      getInstance().deleted = deleted;
      return this;
    }

    public BlockedLabel build() {
      final BlockedLabel instance = getInstance();
      checkArgument(instance.getLabel() != null, "Label must not be null");
      checkArgument(instance.getDateCreated() != null, "Date created must not be null");
      checkArgument(instance.getDateModified() != null, "Date modified must not be null");
      return instance;
    }
  }
}
