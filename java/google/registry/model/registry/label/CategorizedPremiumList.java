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

import java.util.HashMap;
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

  static CategorizedPremiumList savePremiumList(final String tld) {

    // Build a new CategorizedPremiumList
    final CategorizedPremiumList newPremiumList =
        new CategorizedPremiumList.Builder().setName(tld).build().saveAndUpdateEntries();

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

    /**
     * Method assigns the ImmutableMap of CategorizedListEntry objects into the PremiumListMap
     * for this given instance of CategorizedPremiumList
     * @param premiumListMap an ImmutableMap of CategorizedListEntry objects to be assigned
     * @return a Builder object
     */
    public Builder setPremiumListMap(ImmutableMap<String, CategorizedListEntry> premiumListMap) {
      getInstance().entriesWereUpdated = true;
      getInstance().premiumListMap = premiumListMap;
      return this;
    }

    public Builder setPremiumListFromLines(Iterable<String> lines) {
      return setPremiumListMap(getInstance().parse(lines));
    }

    /**
     * Method accepts a second-level domain name and a price category and is to create a
     * CategorizedListEntry object and then add it to the CategorizedPremiumList map
     * @param sld a second-level domain
     * @param priceCategory a price category
     * @return
     */
    public Builder addEntry(final String sld, final String priceCategory) {
      return addEntry(CategorizedListEntry.createEntry(sld, priceCategory));
    }

    /**
     * Method accepts a CategorizedListEntry and determines if it has a list of PremiumListEntries
     * and if it does then it retrieves then otherwise it creates a new ImmutableMap to store
     * @param entry a CategorizedListEntry to load into the PremiumListMap
     * @return a Builder object
     */
    public Builder addEntry(final CategorizedListEntry entry) {
      // Determines if we have a list of PremiumListEntries or not and if
      // not creates an ImmutableMap
      final Map<String, CategorizedListEntry> existingEntries =
          (getInstance().getPremiumListEntries() != null)
              ? getInstance().getPremiumListEntries()
              : ImmutableMap.<String, CategorizedListEntry>of();

      checkState(!existingEntries.containsKey(entry.getLabel()),
          "Entry [%s] already exists", entry.getLabel());

      final ImmutableMap<String, CategorizedListEntry> newEntries =
          ImmutableMap.<String, CategorizedListEntry>builder()
              .putAll(existingEntries) // Adds entries regardless if it has entries or is empty
              .put(entry.getLabel(), entry)
              .build();

      return setPremiumListMap(newEntries);
    }

    /**
     * Method deletes an CategorizedListEntry from the PremiumListMap based upon the
     * second level domain (sld)
     * @param sld a string value representing a second level domain
     * @return
     */
    public Builder deleteEntry(final String sld) {

      final Map<String, CategorizedListEntry> existingEntries =
          getInstance().getPremiumListEntries();

      checkState(existingEntries.containsKey(sld), "Unable to find entry [%s]", sld);

      // Remove entry from existing PremiumListMap
      Map<String, CategorizedListEntry> tmpMap = new HashMap<>(existingEntries);
      tmpMap.remove(sld);

      final ImmutableMap<String, CategorizedListEntry> newEntries =
          ImmutableMap.<String, CategorizedListEntry>builder()
              .putAll(tmpMap)
              .build();

      return setPremiumListMap(ImmutableMap.copyOf(newEntries));
    }

    /**
     * Method updates a CategorizedListEntry from the PremiumListMap based upon the second
     * level domain
     * @param sld a string value representing a second level domain
     * @return a Builder object
     */
    public Builder updateEntry(final String sld,
                               final String priceCategory,
                               final String futureCategory,
                               final DateTime effectiveDate) {

      // Determines if we have a list of PremiumListEntries or not and if
      // not creates an ImmutableMap
      final Map<String, CategorizedListEntry> existingEntries =
          (getInstance().getPremiumListEntries() != null)
              ? getInstance().getPremiumListEntries()
              : ImmutableMap.<String, CategorizedListEntry>of();


      checkState(existingEntries.containsKey(sld), "Unable to find entry for %s", sld);

      Map<String, CategorizedListEntry> tmpMap = new HashMap<>(existingEntries);

      final CategorizedListEntry oldEntry = tmpMap.get(sld);

      CategorizedListEntry updatedEntry=null;

      // Determine if we only have a transition of START_OF_TIME and add new effective date to end
      if(oldEntry.getNextTransitionDateTime() == null) {
        updatedEntry = oldEntry.asBuilder()
            .setPricingCategoryTransitions(ImmutableSortedMap.of(
                START_OF_TIME, oldEntry.getValueAtTime(START_OF_TIME).getName(),
                effectiveDate, futureCategory)).build();
      } else {
        DateTime tmpDate = oldEntry.getNextTransitionDateTime();
        if (tmpDate.getMillis() <= effectiveDate.getMillis()) {

          ImmutableSortedMap<DateTime, String> existingCategoryTransition =
              oldEntry.categoryTransitions.toValueMap();

          Map<DateTime, String> newCategoryTransition = new HashMap<>(existingCategoryTransition);
          DateTime key = existingCategoryTransition.lastKey();

          newCategoryTransition.remove(key);
          newCategoryTransition.put(effectiveDate, futureCategory);



          // Remove entry from existing PremiumListMap
//          Map<String, CategorizedListEntry> tmpMap = new HashMap<>(existingEntries);
//          tmpMap.remove(sld);

//          final ImmutableSortedMap<DateTime, String> newEntries =
//              ImmutableSortedMap.<DateTime, String>builder()
//                  .putAll(newMap)
//                  .build();

          updatedEntry = oldEntry.asBuilder().setPricingCategoryTransitions(
              ImmutableSortedMap.copyOf(newCategoryTransition)
          ).build();


          int x=1;
//          updatedEntry = oldEntry.asBuilder()
//              .setPricingCategoryTransitions(ImmutableSortedMap.<DateTime, String>of().putAll(newMap));
        }
        int x = 1;
      }

      // Remove the existing entry
//      tmpMap.remove(sld);
//      tmpMap.put(sld, updatedEntry);
      Map<String, CategorizedListEntry> newEntries = new HashMap<>();
      newEntries.putAll(existingEntries); // copy over old entries
      newEntries.remove(updatedEntry); // remove old one
      newEntries.put(updatedEntry.getLabel(), updatedEntry); // add new one




//
//      final ImmutableMap<String, CategorizedListEntry> newEntries2 =
//          ImmutableMap.<String, CategorizedListEntry>builder()
//              .put(updatedEntry.getLabel(), updatedEntry)
//              .build();

      return setPremiumListMap(ImmutableMap.copyOf(newEntries));
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
