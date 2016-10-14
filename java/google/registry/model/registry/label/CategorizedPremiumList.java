package google.registry.model.registry.label;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.partition;
import static google.registry.model.common.EntityGroupRoot.getCrossTldKey;
import static google.registry.model.ofy.ObjectifyService.ofy;
import static google.registry.model.ofy.Ofy.RECOMMENDED_MEMCACHE_EXPIRATION;
import static google.registry.util.CollectionUtils.nullToEmpty;
import static google.registry.util.DateTimeUtils.START_OF_TIME;
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
import com.googlecode.objectify.annotation.Ignore;
import com.googlecode.objectify.annotation.Mapify;
import com.googlecode.objectify.annotation.Parent;
import com.googlecode.objectify.cmd.Query;
import google.registry.config.RegistryEnvironment;
import google.registry.model.Buildable;
import google.registry.model.common.TimedTransitionProperty;
import google.registry.model.pricing.PricingCategory;
import google.registry.model.registry.Registry;
import google.registry.util.DateTimeUtils;

import org.joda.money.Money;
import org.joda.time.DateTime;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.annotation.Nullable;

@Entity
@Cache(expirationSeconds = RECOMMENDED_MEMCACHE_EXPIRATION)
public class CategorizedPremiumList
    extends BasePremiumList<String, CategorizedPremiumList.CategorizedListEntry> {

  @Ignore private boolean entriesWereUpdated;

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
   * Method creates a new CategorizedPremiumList based upon the TLD, SLD and PriceCategory
   */
  public static CategorizedPremiumList createPremiumList(
      final String tld, final String sld, final String priceCategory) {

    // Build and return the CategorizedListEntry
    final CategorizedListEntry entry = CategorizedListEntry.createEntry(sld, priceCategory);

    // Create a new ImmutableMap based upon the SLD and PricingCategory
    final ImmutableMap<String, CategorizedListEntry> newEntries =
        ImmutableMap.<String, CategorizedListEntry>builder()
            .put(sld, entry)
            .build();

    return savePremiumList(tld, newEntries);
  }


  /**
   * Method either appends or creates a new CategoryMap within Prem
   */
//  public static CategorizedPremiumList updatePremiumList(
//      CategorizedPremiumList premiumList,
//      final String tld, final String sld, final String priceCategory) {
//
//    ImmutableMap<String, CategorizedListEntry> newEntries;
//    final CategorizedPremiumList newPremiumList;
//
//    // If it is present then retrieve entries and proceed with appending entry to existing list
//    final Map<String, CategorizedListEntry> existingEntries = premiumList.getPremiumListEntries();
//    final CategorizedPremiumList.Builder premiumListBuilder = premiumList.get().asBuilder();
//
//    // Build and return the CategorizedListEntry
//    final CategorizedListEntry entry = CategorizedListEntry.createEntry(sld, priceCategory);
//
//    newEntries =
//        ImmutableMap.<String, CategorizedListEntry>builder()
//            .putAll(existingEntries)
//            .put(sld, entry)
//            .build();
//
//    final CategorizedPremiumList categorizedPremiumList = premiumListBuilder.setPremiumListMap(newEntries).build();
//    categorizedPremiumList.saveAndUpdateEntries();
//
//    ofy().transact(new VoidWork() {
//      @Override
//      public void vrun() {
//        ofy().save().entity(
//            Registry.get(tld)
//                .asBuilder()
//                .setPremiumList(categorizedPremiumList)
//                .build())
//            .now();
//      }
//    });
//
//    return categorizedPremiumList;
//  }


  /**
   * Method deletes a PremiumListEntry from the list of entries associated with PremiumList
   *
   * @return a CategorizedPremiumList which will contain updated
   */
  public static CategorizedPremiumList deletePremiumListEntry(final String tld, final String sld) {

    final CategorizedPremiumList premiumList = getCategorizedPremiumList(tld);

    // Get a modifiable map so we can remove the sld
    final Map<String, CategorizedListEntry> entries =
        Maps.newHashMap(premiumList.getPremiumListEntries());
    checkState(entries.containsKey(sld), "Unable to find entry for %s", sld);

    entries.remove(sld);

    return premiumList.asBuilder()
        .setPremiumListMap(
            ImmutableMap.copyOf(entries))
        .build()
        .saveAndUpdateEntries();
  }


  /**
   * Method adds a future transition date to list of existing Premium List Entries
   */
  public static CategorizedPremiumList updatePremiumList2(
      final String tld, final String sld, final DateTime date, final String futureCategory) {
    //CategorizedPremiumList newPremiumList;

    final CategorizedPremiumList premiumList = getCategorizedPremiumList(tld);

//    final CategorizedPremiumList.Builder premiumListBuilder =
//        new CategorizedPremiumList.Builder().setName(tld);

    // Get a modifiable map so we can remove the sld
    Map<String, CategorizedListEntry> premiumListMap =
        Maps.newHashMap(premiumList.getPremiumListEntries());
    checkState(premiumListMap.containsKey(sld),
        "Unable to find entry for %s", sld);

    // Get the CategorizedListEntry for given SLD
    final CategorizedListEntry oldEntry = premiumListMap.get(sld);

    DateTime nextTransition =
        CategorizedListEntry.getNextTransitionDateTime(oldEntry);

    // Determine if next transition is before date passed in and if it is
    // then it is appended to the end of the transitions otherwise update
    if (nextTransition.getMillis() < date.getMillis()) {
      final ImmutableMap<String, CategorizedListEntry> newMapEntries =
          ImmutableMap.<String, CategorizedListEntry>builder()
              .putAll(premiumListMap) // Shove existing entries into map
              .put(sld, CategorizedListEntry.createEntry(sld, futureCategory)) // now add new entries
                      .build();

      final CategorizedPremiumList finalizedPremiumList = savePremiumList(tld, newMapEntries);


    } else {
      // TODO: Fill this in ask Steve

    }

    return null;
  }

  /**
   * Method saves a CategorizedPremiumList
   * @param tld
   * @param newMapEntries
   */
  static CategorizedPremiumList savePremiumList(
      final String tld, final ImmutableMap<String, CategorizedListEntry> newMapEntries) {

    // Build a new CategorizedPremiumList
    final CategorizedPremiumList.Builder premiumListBuilder =
        new CategorizedPremiumList.Builder().setName(tld);

    final CategorizedPremiumList newPremiumList =
        premiumListBuilder.setPremiumListMap(newMapEntries).build();

    newPremiumList.saveAndUpdateEntries();

    ofy().transact(new VoidWork() {
      @Override
      public void vrun() {
        ofy().save().entity(
            Registry.get(tld)
                .asBuilder()
                .setPremiumList(newPremiumList)
                .build())
            .now();
      }
    });

    return newPremiumList;
  }

  /**
   * Method verifies if TLD is associated with a CategorizedPremiumList and if so returns it.
   */
  static CategorizedPremiumList getCategorizedPremiumList(String tld) {
    final Optional<CategorizedPremiumList> premiumListOptional = CategorizedPremiumList.get(tld);
    checkState(premiumListOptional.isPresent(),
        "Unable to find CategorizedPremiumList for %s", tld);

    // Retrieve the actual PremiumList after verifying existence
    return premiumListOptional.get();
  }

  /**
   * A categorized list entry entity, persisted to Datastore. Each instance represents the price
   * category of a single label on a TLD at a given time.
   */
  @Entity
  @Cache(expirationSeconds = RECOMMENDED_MEMCACHE_EXPIRATION)
  public static class CategorizedListEntry extends DomainLabelEntry<String, CategorizedListEntry>
      implements Buildable {

    @Embed
    public static class PricingCategoryTransition
        extends TimedTransitionProperty.TimedTransition<String> {
      private String pricingCategory;

      @Override
      protected String getValue() {
        final Optional<PricingCategory> pc = PricingCategory.get(pricingCategory);
        checkArgument(pc.isPresent(), "Unable to find pricing category [" + pricingCategory + "]");
        return pc.get().getName();
      }

      @Override
      protected void setValue(String value) {
        this.pricingCategory = value;
      }
    }

    @Parent Key<BasePremiumList.PremiumListRevision> parent;

    @Mapify(TimedTransitionProperty.TimeMapper.class)
    TimedTransitionProperty<String, PricingCategoryTransition> categoryTransitions =
        TimedTransitionProperty.forMapify(
            PricingCategory.UNINITIALIZED, PricingCategoryTransition.class);

    @Override
    public Builder asBuilder() {
      return new Builder(clone(this));
    }

    @Override
    public String getValue() {
      String name = categoryTransitions.getValueAtTime(DateTime.now());
      return name;
    }

    @Nullable
    public DateTime getNextTransitionDateTime() {
      return categoryTransitions.getNextTransitionAfter(DateTime.now());
    }

    public PricingCategory getValueAtTime(DateTime time) {
      String pc = categoryTransitions.getValueAtTime(time);
      return PricingCategory.get(pc).get();
    }

    /**
     * Method creates and returns a CategorizedListEntry object for given SLD and PriceCategory
     * @param sld second level domain
     * @param priceCategory PriceCategory
     * @return a CategorizedListEntry
     */
    static CategorizedListEntry createEntry(String sld, String priceCategory) {
      return new CategorizedListEntry.Builder()
          .setLabel(sld)
          .setPricingCategoryTransitions(
              ImmutableSortedMap.of(
                  START_OF_TIME,
                  priceCategory))
          .build();
    }

    /**
     * Method returns the next date time
     * @param entry
     * @return
     */
    static DateTime getNextTransitionDateTime(CategorizedListEntry entry) {
      return entry.getNextTransitionDateTime();
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

      /**
       * Method sets the Pricing Category Transitions
       */
      public Builder setPricingCategoryTransitions(
          ImmutableSortedMap<DateTime, String> pricingCategoryTransitions) {
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
            ImmutableSortedMap.of(DateTimeUtils.START_OF_TIME, pricingCategory.get().getName()))
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
    // Only update entries if there's actually changes to the entries
    boolean entriesToUpdate = !oldPremiumList.isPresent() || entriesWereUpdated;
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
    final CategorizedPremiumList updated = ofy().transactNew(new Work<CategorizedPremiumList>() {
        @Override
        public CategorizedPremiumList run() {
          final DateTime now = ofy().getTransactionTime();
          final CategorizedPremiumList newList = CategorizedPremiumList.this.asBuilder()
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
    if (!premiumListMap.containsKey(label)) {
      return Optional.absent();
    }
    Optional<PricingCategory> pc = PricingCategory.get(premiumListMap.get(label).getValue());
    if (!pc.isPresent()) {
      return Optional.absent();
    }
    return Optional.of(pc.get().getPrice());
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

    public Builder setPremiumListMap(ImmutableMap<String, CategorizedListEntry> premiumListMap) {
      getInstance().entriesWereUpdated = true;
      getInstance().premiumListMap = premiumListMap;
      return this;
    }

    public Builder setPremiumListFromLines(Iterable<String> lines) {
      return setPremiumListMap(getInstance().parse(lines));
    }

    @Override
    public CategorizedPremiumList build() {
      final CategorizedPremiumList instance = getInstance();

      // Only create a new revision if this is a new instance
      if (getInstance().revisionKey == null) {
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
