package google.registry.model.registry.label;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.partition;
import static google.registry.model.common.EntityGroupRoot.getCrossTldKey;
import static google.registry.model.ofy.ObjectifyService.ofy;
import static google.registry.model.ofy.Ofy.RECOMMENDED_MEMCACHE_EXPIRATION;
import static google.registry.util.CollectionUtils.nullToEmpty;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.VoidWork;
import com.googlecode.objectify.Work;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Embed;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Parent;
import com.googlecode.objectify.cmd.Query;
import google.registry.config.RegistryEnvironment;
import google.registry.model.Buildable;
import google.registry.model.common.TimedTransitionProperty;
import google.registry.model.pricing.PricingCategory;
import google.registry.util.DateTimeUtils;
import org.joda.money.Money;
import org.joda.time.DateTime;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import javax.annotation.Nullable;

@Entity
@Cache(expirationSeconds = RECOMMENDED_MEMCACHE_EXPIRATION)
public class CategorizedPremiumList
    extends BasePremiumList<PricingCategory, CategorizedPremiumList.CategorizedListEntry> {

  private static LoadingCache<String, CategorizedPremiumList> cache = CacheBuilder
      .newBuilder()
      .expireAfterWrite(
          RegistryEnvironment.get().config().getDomainLabelListCacheDuration().getMillis(),
          MILLISECONDS)
      .build(new CacheLoader<String, CategorizedPremiumList>() {
        @Override
        public CategorizedPremiumList load(final String listName) throws Exception {
          return ofy().doTransactionless(new Work<CategorizedPremiumList>() {
            @Override
            public CategorizedPremiumList run() {
              return ofy()
                  .load()
                  .type(CategorizedPremiumList.class)
                  .parent(getCrossTldKey())
                  .id(listName)
                  .now();
            }});
        }});

  public static Optional<CategorizedPremiumList> get(String name) {
    try {
      return Optional.of(cache.get(name));
    } catch (CacheLoader.InvalidCacheLoadException e) {
      return Optional.absent();
    } catch (ExecutionException e) {
      throw new UncheckedExecutionException("Could not retrieve premium list named " + name, e);
    }
  }

  /**
   * A categorized list entry entity, persisted to Datastore. Each instance represents the price
   * category of a single label on a TLD at a given time.
   */
  @Entity
  @Cache(expirationSeconds = RECOMMENDED_MEMCACHE_EXPIRATION)
  public static class CategorizedListEntry
      extends DomainLabelEntry<PricingCategory, CategorizedListEntry> implements Buildable {

    @Embed
    public static class PricingCategoryTransition
        extends TimedTransitionProperty.TimedTransition<PricingCategory> {
      private PricingCategory pricingCategory;

      @Override
      protected PricingCategory getValue() {
        return pricingCategory;
      }

      @Override
      protected void setValue(PricingCategory value) {
        this.pricingCategory = value;
      }
    }

    @Parent Key<BasePremiumList.PremiumListRevision> parent;

    // TODO: Do we need a default pricing category?
    TimedTransitionProperty<PricingCategory, PricingCategoryTransition> categoryTransitions;

    @Override
    public Builder asBuilder() {
      return new Builder(clone(this));
    }

    @Override
    public PricingCategory getValue() {
      return categoryTransitions.getValueAtTime(DateTime.now());
    }

    @Nullable
    public DateTime getNextTransitionDateTime() {
      return categoryTransitions.getNextTransitionAfter(DateTime.now());
    }

    public PricingCategory getValueAtTime(DateTime time) {
      return categoryTransitions.getValueAtTime(time);
    }

    public static class Builder extends DomainLabelEntry.Builder<CategorizedListEntry, Builder> {
      public Builder() {}

      private Builder(CategorizedListEntry instance) {
        super(instance);
      }

      public Builder setParent(Key<BasePremiumList.PremiumListRevision> parentKey) {
        getInstance().parent = parentKey;
        return this;
      }

      public Builder setPricingCategoryTransitions(
          ImmutableSortedMap<DateTime, PricingCategory> pricingCategoryTransitions) {
        getInstance().categoryTransitions =
            TimedTransitionProperty.fromValueMap(
                pricingCategoryTransitions, PricingCategoryTransition.class);
        return this;
      }
    }
  }

  @Nullable
  @Override
  CategorizedListEntry createFromLine(String line) {
    String[] words = line.split(",");
    checkArgument(words.length > 2, "Missing pricing category argument");
    String label = words[1].trim();
    String categoryName = words[2].trim();
    Optional<PricingCategory> pricingCategory = PricingCategory.get(categoryName);
    checkArgument(
            pricingCategory.isPresent(),
            String.format("The pricing category '%s' doesn't exist", categoryName));
    return new CategorizedListEntry.Builder()
            .setLabel(label)
            .setPricingCategoryTransitions(
                    ImmutableSortedMap.of(DateTimeUtils.START_OF_TIME, pricingCategory.get()))
            .build();
  }

  /**
   * Persists a PremiumList object to Datastore.
   *
   * <p>The flow here is: save the new premium list entries parented on that revision entity,
   * save/update the PremiumList, and then delete the old premium list entries associated with the
   * old revision.
   */
  public CategorizedPremiumList saveAndUpdateEntries() {
    final Optional<CategorizedPremiumList> oldPremiumList = get(name);
    // Only update entries if there's actually a new revision of the list to save (which there will
    // be if the list content changes, vs just the description/metadata).
    boolean entriesToUpdate =
        !oldPremiumList.isPresent()
            || !Objects.equals(oldPremiumList.get().revisionKey, this.revisionKey);
    // If needed, save the new child entities in a series of transactions.
    if (entriesToUpdate) {
      for (final List<CategorizedListEntry> batch
          : partition(premiumListMap.values(), TRANSACTION_BATCH_SIZE)) {
        ofy().transactNew(new VoidWork() {
          @Override
          public void vrun() {
            ofy().save().entities(batch);
          }});
      }
    }
    // Save the new PremiumList itself.
    CategorizedPremiumList updated = ofy().transactNew(new Work<CategorizedPremiumList>() {
        @Override
        public CategorizedPremiumList run() {
          DateTime now = ofy().getTransactionTime();
          // Assert that the premium list hasn't been changed since we started this process.
          checkState(
              Objects.equals(
                  ofy()
                    .load()
                    .type(CategorizedPremiumList.class)
                    .parent(getCrossTldKey())
                    .id(name)
                    .now(),
                  oldPremiumList.orNull()),
              "PremiumList was concurrently edited");
          CategorizedPremiumList newList = CategorizedPremiumList.this.asBuilder()
              .setLastUpdateTime(now)
              .setCreationTime(
                  oldPremiumList.isPresent() ? oldPremiumList.get().creationTime : now)
              .build();
          ofy().save().entity(newList);
          return newList;
        }});
    // Update the cache.
    CategorizedPremiumList.cache.put(name, updated);
    // If needed and there are any, delete the entities under the old PremiumList.
    if (entriesToUpdate && oldPremiumList.isPresent()) {
      oldPremiumList.get().deleteEntries();
    }
    return updated;
  }

  @Override
  public Optional<Money> getPremiumPrice(String label) {
    return Optional.fromNullable(
        premiumListMap.containsKey(label) ? premiumListMap.get(label).getValue().getPrice() : null);
  }

  @Override
  protected LoadingCache<String, ?> getCache() {
    return cache;
  }

  @Override
  protected Query<CategorizedListEntry> loadEntriesForCurrentRevision() {
    return ofy().load().type(CategorizedListEntry.class).ancestor(revisionKey);
  }

  @Override
  public Builder asBuilder() {
    return new Builder(clone(this));
  }

  public static class Builder extends BaseDomainLabelList.Builder<CategorizedPremiumList, Builder> {

    public Builder() {}

    private Builder(CategorizedPremiumList instance) {
      super(instance);
    }

    private boolean entriesWereUpdated;

    public Builder setPremiumListMap(ImmutableMap<String, CategorizedListEntry> premiumListMap) {
      entriesWereUpdated = true;
      getInstance().premiumListMap = premiumListMap;
      return this;
    }

    public Builder setPremiumListFromLines(Iterable<String> lines) {
      return setPremiumListMap(getInstance().parse(lines));
    }

    @Override
    public CategorizedPremiumList build() {
      final CategorizedPremiumList instance = getInstance();
      if (getInstance().revisionKey == null || entriesWereUpdated) {
        getInstance().revisionKey = PremiumListRevision.createKey(instance);
      }

      instance.premiumListMap =
          Maps.transformValues(
              nullToEmpty(instance.premiumListMap),
              new Function<CategorizedListEntry, CategorizedListEntry>() {
                @Override
                public CategorizedListEntry apply(CategorizedListEntry entry) {
                  return entry.asBuilder().setParent(instance.revisionKey).build();
                }
              });
      return super.build();
    }
  }
}
