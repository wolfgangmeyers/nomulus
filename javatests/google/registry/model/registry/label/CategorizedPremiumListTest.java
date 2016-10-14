package google.registry.model.registry.label;

import static com.google.common.truth.Truth.assertThat;
import static google.registry.model.ofy.ObjectifyService.ofy;
import static google.registry.testing.DatastoreHelper.createTld;
import static google.registry.testing.DatastoreHelper.persistCategorizedPremiumList;
import static google.registry.testing.DatastoreHelper.persistPricingCategory;
import static google.registry.testing.DatastoreHelper.persistResource;
import static google.registry.util.DateTimeUtils.START_OF_TIME;
import static org.joda.money.CurrencyUnit.USD;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;

import com.googlecode.objectify.VoidWork;

import google.registry.model.pricing.PricingCategory;
import google.registry.model.registry.Registry;
import google.registry.model.registry.label.CategorizedPremiumList.CategorizedListEntry;
import google.registry.testing.AppEngineRule;
import google.registry.testing.ExceptionRule;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/** Unit tests for {@link CategorizedPremiumList} */
public class CategorizedPremiumListTest {

  private static final String US_PRICE_CATEGORY = "US_PRICE_CATEGORY";
  private static final String EURO_PRICE_CATEGORY = "EURO_PRICE_CATEGORY";
  private static final String JAPANESE_PRICE_CATEGORY = "JAPANESE_PRICE_CATEGORY";
  private static final String JAPANESE_PRICE = CurrencyUnit.JPY + " 511";
  private static final String USD_PRICE = CurrencyUnit.USD + " 5.00";
  private static final String EURO_PRICE = CurrencyUnit.EUR + " 4.48";
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

  @Before
  public void before() throws Exception {
    PricingCategory pc = persistPricingCategory(US_PRICE_CATEGORY, USD_PRICE);
    PricingCategory pc2 = persistPricingCategory(EURO_PRICE_CATEGORY, EURO_PRICE);
    PricingCategory pc3 = persistPricingCategory(JAPANESE_PRICE_CATEGORY, JAPANESE_PRICE);

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
    CategorizedPremiumList result = CategorizedPremiumList.updatePremiumList2(
        TLD_ONE, LABEL_ONE, FUTURE_DATE, FUTURE_CATEGORY);
    assertThat(result).isEqualTo(CategorizedPremiumList.class);
  }

  @Test
  public void testDeleteTransition_Valid() throws Exception {
    final String sld3 = "sld3";
    final String tld3 = "tld3";

    // Need to create a temporary TLD and then delete
    TldCreator.build(sld3, tld3);

    // Validate data
    CategorizedPremiumList result = CategorizedPremiumList.deletePremiumListEntry(
        tld3, sld3);
    assertThat(result.getPremiumListEntries()).hasSize(0);
  }

  @Test
  public void testDeleteTransition_Invalid_TldDoesNotExist() throws Exception {
    final String sld3 = "sld3";
    final String tld3 = "invalid";

    thrown.expect(IllegalStateException.class, "Unable to find CategorizedPremiumList for invalid");
    CategorizedPremiumList.deletePremiumListEntry(tld3, sld3);
  }

  @Test
  public void testGetCategorizedPremiumList_Valid() throws Exception {
      CategorizedPremiumList result =
          CategorizedPremiumList.getCategorizedPremiumList(TLD_ONE);
    assertThat(result).isNotNull();
  }

  @Test
  public void testGetCategorizedPremiumList_Invalid_UnableToFindList() throws Exception {
    final String sld2 = "sld2";
    final String tld2 = "tld2";
    final String fqdn2 = sld2 + "." + tld2;

    // Need to create a temporary TLD in order to get past .build()
    // and then delete prior to calling .run()
    // Create second TLD and
    TldCreator.build(sld2, tld2);

    // Remove from data store prior to calling result.run()
    ofy().transact(new VoidWork() {
      @Override
      public void vrun() {
        ofy().delete().entity(CategorizedPremiumList.get(tld2).get()).now();
      }
    });

    // Invoke the 'deleteCategorizedListEntries()' which will throw IllegalStateException
    thrown.expect(IllegalStateException.class, "Unable to find CategorizedPremiumList for " + tld2);

    CategorizedPremiumList result =
        CategorizedPremiumList.getCategorizedPremiumList(TLD_ONE);
    assertThat(result).isNotNull();
  }

  @Test
  public void testUpdatePremiumList_Valid_ExistingPremiumListEntries() throws Exception {
//    CategorizedPremiumList result =
//        CategorizedPremiumList.updatePremiumList2(TLD_ONE, LABEL_ONE, US_PRICE_CATEGORY);
//    assertThat(result).isNotNull();
  }

  @Test
  public void testCreatePremiumList_Valid_NoExistingPremiumListEntries() throws Exception {
    CategorizedPremiumList result =
        CategorizedPremiumList.createPremiumList(TLD_ONE, "sld6", US_PRICE_CATEGORY);
    assertThat(result).isNotNull();
  }


  @Test
  public void testDeleteTransition_Invalid_UnableToFindCpl() throws Exception {
//    final String sld2 = "sld2";
//    final String tld2 = "tld2";
//    final String fqdn2 = sld2 + "." + tld2;
//
//    // Need to create a temporary TLD in order to get past .build()
//    // and then delete prior to calling .run()
//    // Create second TLD and
//    TldCreator.build(sld2, tld2);
//
//    // Validate data
//    CategorizedPremiumOperation result = CategorizedPremiumOperation.builder()
//        .operation(D)
//        .fqdn(fqdn2)
//        .pricingCategory("")  // Ignored
//        .effectiveDate("")  // Ignored
//        .futureCategory("") // Ignored
//        .build();
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
//    thrown.expect(IllegalStateException.class, "Unable to find CategorizedPremiumList for " + tld2);
//    result.run();
  }

  @Test
  public void testDeleteTransition_Invalid_UnableToFindEntry() throws Exception {
//    final String sld4 = "sld4";
//    final String tld4 = "tld4";
//    final String fqdn4 = sld4 + "." + tld4;
//
//    // Need to create a temporary TLD in order to get past .build()
//    // and then delete prior to calling .run()
//    // Create second TLD and
//    TldCreator.build(sld4, tld4);
//
//    // Validate data
//    CategorizedPremiumOperation result = CategorizedPremiumOperation.builder()
//        .operation(D)
//        .fqdn(fqdn4)
//        .pricingCategory("")  // Ignored
//        .effectiveDate("")    // Ignored
//        .futureCategory("")   // Ignored
//        .build();
//
//    // Purposely remove the CategorizedPremiumListEntries to ensure exception is thrown
//    removePremiumListEntries(sld4, tld4);
//
//    // Invoke the 'deleteCategorizedListEntries()' which will throw IllegalStateException
//    thrown.expect(IllegalStateException.class, "Unable to find entry for " + sld4);
//    result.run();
  }




  @Test
  public void testAddFutureTransition_Invalid_UnableToFindCpl() throws Exception {
//    final String sld3 = "sld3";
//    final String tld3 = "tld3";
//    final String fqdn3 = sld3 + "." + tld3;
//
//    // Need to create a temporary TLD in order to get past .build()
//    // and then delete prior to calling .run()
//    TldCreator.build(sld3, tld3);
//    final PricingCategory pc = TldCreator.getPricingCategory();
//
//    // Validate data
//    CategorizedPremiumList result = CategorizedPremiumList.addFutureTransition(
//        tld3, sld3, FUTURE_DATE, FUTURE_CATEGORY);
//
//    // Remove from data store prior to calling result.run()
//    ofy().transact(new VoidWork() {
//      @Override
//      public void vrun() {
//        ofy().delete().entity(CategorizedPremiumList.get(tld3).get()).now();
//      }
//    });
//
//    // Invoke the 'deleteCategorizedListEntries()' which will throw IllegalStateException
//    thrown.expect(IllegalStateException.class, "Unable to find CategorizedPremiumList for " + tld3);
  }

  @Test
  public void testUpdateCategorizedListEntries_Invalid_UnableToFindEntry() throws Exception {
//    final String sld5 = "sld5";
//    final String tld5 = "tld5";
//    final String fqdn5 = sld5 + "." + tld5;
//
//    // Need to create a temporary TLD in order to get past .build()
//    // and then delete prior to calling .run()
//    // Create second TLD and
//    TldCreator.build(sld5, tld5);
//
//    // Validate data before issuing the result.run()
//    CategorizedPremiumOperation result = CategorizedPremiumOperation.builder()
//        .operation(U)
//        .fqdn(fqdn5)
//        .pricingCategory(TldCreator.getPricingCategory().getName())  // Ignored
//        .effectiveDate(FUTURE_DATE.toString(formatter))  // Ignored
//        .futureCategory(futureCategory) // Ignored
//        .build();
//
//    // Purposely remove the CategorizedPremiumListEntries to ensure exception is thrown
//    removePremiumListEntries(sld5, tld5);
//
//    // Invoke the 'deleteCategorizedListEntries()' which will throw IllegalStateException
//    thrown.expect(IllegalStateException.class, "Unable to find entry for " + sld5);
//    result.run();
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
