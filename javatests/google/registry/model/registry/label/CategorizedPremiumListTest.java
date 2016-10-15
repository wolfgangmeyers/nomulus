package google.registry.model.registry.label;

import static com.google.common.truth.Truth.assertThat;
import static google.registry.model.registry.label.CategorizedPremiumList.CategorizedListEntry.createEntry;
import static google.registry.testing.DatastoreHelper.createTld;
import static google.registry.testing.DatastoreHelper.persistCategorizedPremiumList;
import static google.registry.testing.DatastoreHelper.persistPricingCategory;
import static google.registry.testing.DatastoreHelper.persistResource;
import static google.registry.util.DateTimeUtils.START_OF_TIME;
import static org.joda.money.CurrencyUnit.USD;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;

import google.registry.model.pricing.PricingCategory;
import google.registry.model.registry.Registry;
import google.registry.model.registry.label.CategorizedPremiumList.CategorizedListEntry;
import google.registry.testing.AppEngineRule;
import google.registry.testing.ExceptionRule;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

/** Unit tests for {@link CategorizedPremiumList} */
public class CategorizedPremiumListTest {

  private static final String US_PRICE_CATEGORY = "US_PRICE_CATEGORY";
  private static final String EURO_PRICE_CATEGORY = "EURO_PRICE_CATEGORY";
  private static final String JAPANESE_PRICE_CATEGORY = "JAPANESE_PRICE_CATEGORY";
  private static final String SWISS_PRICE_CATEGORY = "SWISS_PRICE_CATEGORY";
  private static final String JAPANESE_PRICE = CurrencyUnit.JPY + " 511";
  private static final String USD_PRICE = CurrencyUnit.USD + " 5.00";
  private static final String EURO_PRICE = CurrencyUnit.EUR + " 4.48";
  private static final String SWISS_PRICE = CurrencyUnit.CHF + "4.95";
  private static final String LABEL_ONE = "label_one";
  private static final String LABEL_TWO = "label_two";
  private static final String TLD_ONE = "tld_one";
  private static final String TLD_TWO = "tld_two";
  private static final String FUTURE_CATEGORY = "fc";

  private final DateTime THREE_DAYS = DateTime.now().plusDays(3);
  private final DateTime FIVE_DAYS = DateTime.now().plusDays(5);
  private final DateTime FUTURE_DATE = new DateTime().plusDays(5);

  @Rule public final ExceptionRule thrown = new ExceptionRule();
  @Rule public final AppEngineRule appEngine = AppEngineRule.builder().withDatastore().build();

  private CategorizedPremiumList premiumList;
  private CategorizedPremiumList nullablePremiumList;
  private PricingCategory swissPriceCategory;
  private PricingCategory pricingCategory_AA;
  private PricingCategory pricingCategory_AA_Plus;
  private PricingCategory pricingCategory_B;
  private PricingCategory pricingCategory_BB;
  private PricingCategory pricingCategory_CCCC;

  @Before
  public void before() throws Exception {
    PricingCategory pc = persistPricingCategory(US_PRICE_CATEGORY, USD_PRICE);
    PricingCategory pc2 = persistPricingCategory(EURO_PRICE_CATEGORY, EURO_PRICE);
    PricingCategory pc3 = persistPricingCategory(JAPANESE_PRICE_CATEGORY, JAPANESE_PRICE);
    swissPriceCategory = persistPricingCategory(SWISS_PRICE_CATEGORY, SWISS_PRICE);
    pricingCategory_AA_Plus = persistPricingCategory("AA+", USD_PRICE);
    pricingCategory_AA = persistPricingCategory("AA", USD_PRICE);
    pricingCategory_B = persistPricingCategory("B", USD_PRICE);
    pricingCategory_BB = persistPricingCategory("BB", USD_PRICE);
    pricingCategory_CCCC = persistPricingCategory("CCCC", USD_PRICE);


    // Adding three pricing categories with three different dates purposely to verify date
    // ordering for testing method 'getNextTransitionDateTime'
    premiumList =
        persistCategorizedPremiumList(
            ImmutableSortedMap.of(
                FIVE_DAYS, pc.getName(), START_OF_TIME, pc2.getName(), THREE_DAYS, pc3.getName()),
            TLD_ONE,
            LABEL_ONE);

    // Second Premium List for testing for the @Nullable of method 'getNextTransitionDateTime'
    nullablePremiumList = persistCategorizedPremiumList(
        ImmutableSortedMap.of(START_OF_TIME, pc.getName()), TLD_TWO, LABEL_TWO);
  }

  @Test
  public void testCreateFromLine_shouldHandleCreate() {
    CategorizedListEntry entry =
        premiumList.createFromLine("A, car, " + US_PRICE_CATEGORY);
    assertThat(entry.getValue()).isEqualTo(US_PRICE_CATEGORY);
  }

  @Test
  public void testCreateFromLine_shouldThrowIllegalArgument_whenLessThan3Fields() {
    thrown.expect(IllegalArgumentException.class, "Missing pricing category argument");
    premiumList.createFromLine("A, car");
  }

  @Test
  public void testCreateFromLine_shouldThrowIllegalArgument_whenPricingCategoryDoesNotExist() {
    thrown.expect(
        IllegalArgumentException.class, "The pricing category 'DoesNotExist' doesn't exist");
    premiumList.createFromLine("A, car, DoesNotExist");
  }

  @Test
  public void testGetNextTransitionDateTime_futureDate() {
    CategorizedListEntry entry =
        premiumList.getPremiumListEntries().get(LABEL_ONE);
    DateTime nextTransitionDateTime = entry.getNextTransitionDateTime();
    PricingCategory futurePriceCategory = entry.getValueAtTime(nextTransitionDateTime);

    assertThat(nextTransitionDateTime).isEqualTo(THREE_DAYS);
    assertThat(futurePriceCategory.getName()).isEqualTo(JAPANESE_PRICE_CATEGORY);
    assertThat(futurePriceCategory.getPrice().toString()).isEqualTo(JAPANESE_PRICE);
  }

  @Test
  public void testGetNextTransitionDateTime_whenTransitionDateTimeIsNullable() {
    CategorizedListEntry entry =
        nullablePremiumList.getPremiumListEntries().get(LABEL_ONE);
    thrown.expect(NullPointerException.class);
    entry.getNextTransitionDateTime();
  }

  @Test
  public void testGetPremiumPrice_shouldReturnCurrentPriceForLabel() {
    Registry registry = Registry.get(TLD_ONE);
    CategorizedPremiumList premiumList = CategorizedPremiumList.get(registry.getPremiumList()
                                                                        .getName()).get();
    Optional<Money> result = premiumList.getPremiumPrice(LABEL_ONE);
    Money expected = Money.parse(EURO_PRICE);

    assertThat(result).isPresent();
    assertThat(result.get()).isEqualTo(expected);
  }

  @Test
  public void updateEntitiesSameRevision() {
    // Add to the existing entries
    final ImmutableMap<String, CategorizedListEntry> updatedEntries =
        ImmutableMap.<String, CategorizedListEntry>builder()
            .putAll(premiumList.getPremiumListEntries())
            .put("test",
                new CategorizedListEntry.Builder()
                    .setLabel("test")
                    .setPricingCategoryTransitions(
                        ImmutableSortedMap.of(START_OF_TIME, US_PRICE_CATEGORY))
                    .build())
            .build();

    // Save and update should save the new entries while persisting the revision number
    final CategorizedPremiumList updatedPremiumList = premiumList.asBuilder()
        .setPremiumListMap(updatedEntries)
        .build()
        .saveAndUpdateEntries();

    // Verify the revision key has not changed
    assertThat(premiumList.getRevisionKey()).isEqualTo(updatedPremiumList.getRevisionKey());

    // Verify the new entity has been added to the existing revision
    assertThat(updatedPremiumList.getPremiumListEntries().size())
        .isEqualTo(premiumList.getPremiumListEntries().size() + 1);
  }

  @Ignore
  @Test
  public void testUpdateFutureTransition_Valid() throws Exception {
    final String sld3 = "sld3";
    final String tld3 = "tld3";
    final String fqdn3 = sld3 + "." + tld3;

    // Need to create a temporary TLD in order to get past .build()
    // and then delete prior to calling .run()
    TldCreator.build(sld3, tld3);
    final PricingCategory pc = TldCreator.getPricingCategory();

    // Validate data
//    CategorizedPremiumList result = CategorizedPremiumList.updatePremiumList2(
//        TLD_ONE, LABEL_ONE, FUTURE_DATE, FUTURE_CATEGORY);
//    assertThat(result).isEqualTo(CategorizedPremiumList.class);
  }

  @Ignore
  @Test
  public void testDeletePremiumListEntry_Valid() throws Exception {
//    final String sld3 = "sld3";
//    final String tld3 = "tld3";
//
//    // Need to create a temporary TLD and then delete
//    TldCreator.build(sld3, tld3);
//
//    // Validate data
//    CategorizedPremiumList result = CategorizedPremiumList.deletePremiumListEntry(
//        tld3, sld3);
//    assertThat(result.getPremiumListEntries()).hasSize(0);
  }

  @Ignore
  @Test
  public void testDeletePremiumListEntry_Invalid_TldDoesNotExist() throws Exception {
//    final String sld3 = "sld3";
//    final String tld3 = "invalid";
//
//    thrown.expect(IllegalStateException.class, "Unable to find CategorizedPremiumList for invalid");
//    CategorizedPremiumList.deletePremiumListEntry(tld3, sld3);
  }

  @Ignore
  @Test
  public void testGetCategorizedPremiumList_Valid() throws Exception {
//      CategorizedPremiumList result =
//          CategorizedPremiumList.getCategorizedPremiumList(TLD_ONE);
//    assertThat(result).isNotNull();
  }

  @Ignore
  @Test
  public void testGetCategorizedPremiumList_Invalid_UnableToFindList() throws Exception {
//    final String sld2 = "sld2";
//    final String tld2 = "tld2";
//    final String fqdn2 = sld2 + "." + tld2;
//
//    // Need to create a temporary TLD in order to get past .build()
//    // and then delete prior to calling .run()
//    // Create second TLD and
//    TldCreator.build(sld2, tld2);
//
//    // Remove from data store prior to calling result.run()
//    ofy().transact(new VoidWork() {
//      @Override
//      public void vrun() {
//        ofy().delete().entity(CategorizedPremiumList.get(tld2).get()).now();
//      }
//    });
//
//    // Invoke the 'deleteCategorizedListEntries()' which will throw IllegalStateException
//    thrown.expect(IllegalStateException.class,
//        "Unable to find CategorizedPremiumList for " + tld2);
//
//    CategorizedPremiumList result =
//        CategorizedPremiumList.getCategorizedPremiumList(TLD_TWO);
  }

  @Ignore
  @Test
  public void testSavePremiumList() throws Exception {
    final String sld = "sld8";
    final CategorizedListEntry entry =
        createEntry(sld, US_PRICE_CATEGORY);

    // Create a new ImmutableMap based upon the SLD and PricingCategory
    final ImmutableMap<String, CategorizedListEntry> newEntries =
        ImmutableMap.<String, CategorizedListEntry>builder()
            .put(sld, entry)
            .build();

//    CategorizedPremiumList result =
//        CategorizedPremiumList.savePremiumList(TLD_ONE, newEntries);
//    assertThat(result.getPremiumListEntries().get(sld)).isNotNull();
  }


  @Test
  public void testAddEntry_Valid() throws Exception {
    /**
     * Test method exercises the functionality of both 'addEntry' methods that accept the following
     * addEntry(final String sld, final String priceCategory) and
     * addEntry(final CategorizedListEntry entry)
     *
     * and verifies that the result values are correct
     */
    final String sld = "sld";
    final String sld3 = "sld3";
    final CategorizedListEntry us_entry = createEntry(sld, US_PRICE_CATEGORY);
    final CategorizedListEntry jap_entry = createEntry(sld3, JAPANESE_PRICE_CATEGORY);


    final CategorizedPremiumList result = new CategorizedPremiumList.Builder()
        .setName("tld")
        .addEntry(us_entry)
        .addEntry(sld3, JAPANESE_PRICE_CATEGORY)
        .build();

    assertThat(result.getPremiumListEntries()).containsExactly(
        sld,
        us_entry.asBuilder()
            .setParent(result.getRevisionKey())
            .build(),
        sld3,
        jap_entry.asBuilder()
            .setParent(result.getRevisionKey())
            .build());
  }

  @Test
  public void testAddEntry_Valid_ExistingPremiumList() {
    /**
     * Test method exercises the functionality of both 'addEntry' methods however it is using
     * a pre-loaded CategorizedPremiumList and verifies that the result does in fact contain
     * the new CategorizedListEntry objects in it
     */

    final String washington_sld = "washington";
    final String oregon_sld = "oregon";
    final String doctor_tld = "doctor";

    // create a pre-loaded premium list and then add an entry
    final CategorizedPremiumList preloadedPremiumList =
        persistCategorizedPremiumList(
            ImmutableSortedMap.of(
                FIVE_DAYS, pricingCategory_AA.getName(),
                START_OF_TIME, pricingCategory_CCCC.getName(),
                THREE_DAYS, pricingCategory_BB.getName()),
            doctor_tld,
            washington_sld);

    // create a temporary entry to compare against
    final CategorizedListEntry oregon_entry = createEntry(oregon_sld, US_PRICE_CATEGORY);

    // TODO: Use Builder - > get a pre-loaded PremiumList

    final CategorizedPremiumList result = preloadedPremiumList.asBuilder()
        //.addEntry(washington_doctor_entry) // washington.doctor
        .addEntry(oregon_sld, US_PRICE_CATEGORY) // oregon.doctor
        .build();

    // Should now have two entries in map
    assertThat(result.getPremiumListEntries().size()).isEqualTo(2);

    // Verify that Oregon is one of the entries
    assertThat(result.getPremiumListEntries().get(oregon_sld))
        .isEquivalentAccordingToCompareTo(oregon_entry);
  }

  @Test
  public void testDeleteEntry_Valid() throws Exception {
    /**
     * Test method exercises the functionality of both 'deleteEntry' methods however it is using
     * a pre-loaded CategorizedPremiumList and verifies that the result does in fact contain
     * the new CategorizedListEntry objects in it
     */

    final String mountain_sld = "mountain";
    final String road_sld = "road";
    final String tandem_sld = "tandem";
    final String bike_tld = "bike";


    // create a pre-loaded premium list and then add an entry
    final CategorizedPremiumList bicyclePremiumList =
        persistCategorizedPremiumList(
            ImmutableSortedMap.of(
                FIVE_DAYS, pricingCategory_AA.getName(),
                START_OF_TIME, pricingCategory_CCCC.getName(),
                THREE_DAYS, pricingCategory_BB.getName()),
            bike_tld,
            mountain_sld);  // mountain.bike

    // create a temporary entry to compare against
    final CategorizedListEntry road_bike_entry = createEntry(road_sld, US_PRICE_CATEGORY);

    // Add more CategorizedListEntry objects into PremiumList
    final CategorizedPremiumList threeBicycles = bicyclePremiumList.asBuilder()
        .addEntry(road_sld, US_PRICE_CATEGORY) // road.bike
        .addEntry(tandem_sld, US_PRICE_CATEGORY) // tandem.bike
        .build();

    // Should now have two entries in map
    assertThat(threeBicycles.getPremiumListEntries().size()).isEqualTo(3);

    // Delete the road bicycle
    CategorizedPremiumList twoBicycles =
        threeBicycles.asBuilder()
            .deleteEntry(road_bike_entry)
            .build();

    // Verify that we have two bicycles left - Tandem, Mountain
    assertThat(twoBicycles.getPremiumListEntries().size()).isEqualTo(2);

    assertThat(twoBicycles.getPremiumListEntries().get(tandem_sld))
        .isEquivalentAccordingToCompareTo(createEntry(tandem_sld, US_PRICE_CATEGORY));

    assertThat(twoBicycles.getPremiumListEntries().containsKey(mountain_sld)).isTrue();
  }

  @Test
  public void testDeleteEntry_Valid_SLDAndPriceCategory() throws Exception {
    /**
     * Test method exercises the functionality of both 'deleteEntry' methods however it is using
     * a pre-loaded CategorizedPremiumList and verifies that the result does in fact contain
     * the new CategorizedListEntry objects in it
     */

    final String elementary_sld = "elementary";
    final String middle_sld = "middle";
    final String senior_high_sld = "senior-high";
    final String school_tld = "school";


    // create a pre-loaded premium list and then add an entry
    final CategorizedPremiumList bicyclePremiumList =
        persistCategorizedPremiumList(
            ImmutableSortedMap.of(
                FIVE_DAYS, pricingCategory_AA.getName(),
                START_OF_TIME, pricingCategory_CCCC.getName(),
                THREE_DAYS, pricingCategory_BB.getName()),
            school_tld,
            elementary_sld);  // elementary.school

    // create a temporary entry to compare against
    final CategorizedListEntry middle_school_entry = createEntry(middle_sld, US_PRICE_CATEGORY);

    // Add more CategorizedListEntry objects into PremiumList
    final CategorizedPremiumList threeSchools = bicyclePremiumList.asBuilder()
        .addEntry(middle_sld, US_PRICE_CATEGORY) // middle.school
        .addEntry(senior_high_sld, US_PRICE_CATEGORY) // senior-high.school
        .build();

    // Should now have two entries in map
    assertThat(threeSchools.getPremiumListEntries().size()).isEqualTo(3);

    // Delete the road bicycle
    CategorizedPremiumList twoSchools =
        threeSchools.asBuilder()
            .deleteEntry(middle_school_entry)
            .build();

    // Verify that we have two bicycles left - Tandem, Mountain
    assertThat(twoSchools.getPremiumListEntries().size()).isEqualTo(2);

    assertThat(twoSchools.getPremiumListEntries().get(senior_high_sld))
        .isEquivalentAccordingToCompareTo(createEntry(senior_high_sld, US_PRICE_CATEGORY));

    assertThat(twoSchools.getPremiumListEntries().containsKey(elementary_sld)).isTrue();
  }

  /**
   * Class is used to support the creation of temporary TLDs and dependent entities used
   * for specific test cases that require a TLD to be created and then deleted during
   * test case run
   */
  private static class TldCreator {
    private static String pricingCategoryName = "pricingCategory";
    private static PricingCategory pc;
    private static CategorizedPremiumList.CategorizedListEntry entry;

    public static PricingCategory getPricingCategory() {
      return pc;
    }

    public static CategorizedPremiumList.CategorizedListEntry getEntry() {
      return entry;
    }

    public static void build(String sld, String tld) {
      createTld(tld);

      pc = new PricingCategory.Builder().setName(pricingCategoryName).setPrice(Money.zero(USD)).build();
      persistResource(pc);

      entry =
          new CategorizedPremiumList.CategorizedListEntry.Builder()
              .setLabel(sld)
              .setPricingCategoryTransitions(ImmutableSortedMap.of(START_OF_TIME, pc.getName()))
              .build();

      new CategorizedPremiumList.Builder()
          .setName(tld)
          .setPremiumListMap(ImmutableMap.of(sld, entry))
          .build()
          .saveAndUpdateEntries();
    }
  }
}
