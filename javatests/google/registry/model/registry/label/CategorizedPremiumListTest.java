package google.registry.model.registry.label;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import google.registry.model.pricing.PricingCategory;
import google.registry.model.registry.Registry;
import google.registry.testing.AppEngineRule;
import google.registry.testing.ExceptionRule;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;
import static google.registry.testing.DatastoreHelper.createTld;
import static google.registry.testing.DatastoreHelper.persistResource;
import static google.registry.util.DateTimeUtils.START_OF_TIME;

/** Unit tests for {@link CategorizedPremiumList} */
public class CategorizedPremiumListTest {

  private static final String US_PRICE_CATEGORY = "US_PRICE_CATEGORY";
  private static final String EURO_PRICE_CATEGORY = "EURO_PRICE_CATEGORY";
  private static final String JAPANESE_PRICE_CATEGORY = "JAPANESE_PRICE_CATEGORY";

  private final DateTime THREE_DAYS = DateTime.now().plusDays(3);
  private final DateTime FIVE_DAYS = DateTime.now().plusDays(5);
  private static final String JAPANESE_PRICE = CurrencyUnit.JPY + " 511";
  private static final String USD_PRICE = CurrencyUnit.USD + " 5.00";
  private static final String EURO_PRICE = CurrencyUnit.EUR + " 4.48";
  private static final String LABEL_ONE = "label_one";
  private static final String LABEL_TWO = "label_two";
  private static final String TLD_ONE = "tld_one";
  private static final String TLD_TWO = "tld_two";

  @Rule public final ExceptionRule thrown = new ExceptionRule();
  @Rule public final AppEngineRule appEngine = AppEngineRule.builder().withDatastore().build();

  private CategorizedPremiumList premiumList;
  private CategorizedPremiumList nullablePremiumList;

  @Before
  public void before() throws Exception {
    PricingCategory pc = buildPricingCategory(US_PRICE_CATEGORY, USD_PRICE);
    PricingCategory pc2 = buildPricingCategory(EURO_PRICE_CATEGORY, EURO_PRICE);
    PricingCategory pc3 = buildPricingCategory(JAPANESE_PRICE_CATEGORY, JAPANESE_PRICE);

    persistResource(pc);
    persistResource(pc2);
    persistResource(pc3);

    // Adding three pricing categories with three different dates purposely to verify date
    // ordering for testing method 'getNextTransitionDateTime'
    premiumList =
        buildPremiumList(
            ImmutableSortedMap.of(FIVE_DAYS, pc, START_OF_TIME, pc2, THREE_DAYS, pc3),
            TLD_ONE,
            LABEL_ONE);
    createTld(TLD_ONE);
    persistResource(Registry.get(TLD_ONE).asBuilder().setPremiumList(premiumList).build());

    // Second Premium List for testing for the @Nullable of method 'getNextTransitionDateTime'
    nullablePremiumList =
        buildPremiumList(ImmutableSortedMap.of(START_OF_TIME, pc), TLD_TWO, LABEL_TWO);
    createTld(TLD_TWO);
    persistResource(Registry.get(TLD_TWO).asBuilder().setPremiumList(nullablePremiumList).build());
  }

  @Test
  public void testCreateFromLine_shouldHandleCreate() {
    CategorizedPremiumList.CategorizedListEntry entry =
        premiumList.createFromLine("A, car, " + US_PRICE_CATEGORY);
    assertThat(entry.getValue().getName()).isEqualTo(US_PRICE_CATEGORY);
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
    CategorizedPremiumList.CategorizedListEntry entry =
        premiumList.getPremiumListEntries().get(LABEL_ONE);
    DateTime nextTransitionDateTime = entry.getNextTransitionDateTime();
    PricingCategory futurePriceCategory = entry.getValueAtTime(nextTransitionDateTime);

    assertThat(nextTransitionDateTime).isEqualTo(THREE_DAYS);
    assertThat(futurePriceCategory.getName()).isEqualTo(JAPANESE_PRICE_CATEGORY);
    assertThat(futurePriceCategory.getPrice().toString()).isEqualTo(JAPANESE_PRICE);
  }

  @Test
  public void testGetNextTransitionDateTime_whenTransitionDateTimeIsNullable() {
    CategorizedPremiumList.CategorizedListEntry entry =
        nullablePremiumList.getPremiumListEntries().get(LABEL_ONE);
    thrown.expect(NullPointerException.class);
    entry.getNextTransitionDateTime();
  }

  /**
   * Method returns a CategorizedPremiumList to allow for multiple to be built out for testing
   * purposes
   *
   * @param map An ImmutableSortedMap that represents a DateTime value and associated
   *     PricingCategory
   * @param tld A string value representing a top level domain
   * @param label A string value representing a key for the map
   * @return A CategorizedPremiumList object
   */
  private CategorizedPremiumList buildPremiumList(
      ImmutableSortedMap<DateTime, PricingCategory> map, String tld, String label) {

    return new CategorizedPremiumList.Builder()
        .setName(tld)
        .setPremiumListMap(
            ImmutableMap.of(
                label,
                new CategorizedPremiumList.CategorizedListEntry.Builder()
                    .setLabel(label)
                    .setPricingCategoryTransitions(map)
                    .build()))
        .build();
  }

  /**
   * Method returns a PricingCategory object
   *
   * @param category A string value representing the category
   * @param money A sting value representing the money "USD 5.00"
   * @return A PricingCategory object
   */
  private PricingCategory buildPricingCategory(String category, String money) {
    return new PricingCategory.Builder().setName(category).setPrice(Money.parse(money)).build();
  }
}
