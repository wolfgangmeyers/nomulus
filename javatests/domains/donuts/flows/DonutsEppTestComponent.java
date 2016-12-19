package domains.donuts.flows;

import com.google.appengine.api.modules.ModulesService;
import dagger.Component;
import dagger.Module;
import dagger.Provides;
import dagger.Subcomponent;
import domains.donuts.config.DonutsConfigModule;
import domains.donuts.flows.custom.DonutsCustomLogicFactory;
import google.registry.dns.DnsQueue;
import google.registry.flows.EppController;
import google.registry.flows.FlowComponent;
import google.registry.flows.custom.CustomLogicFactory;
import google.registry.monitoring.whitebox.BigQueryMetricsEnqueuer;
import google.registry.monitoring.whitebox.EppMetric;
import google.registry.request.RequestScope;
import google.registry.testing.FakeClock;
import google.registry.testing.FakeSleeper;
import google.registry.util.Clock;
import google.registry.util.Sleeper;

import javax.inject.Singleton;

import static org.mockito.Mockito.mock;

/** Dagger component for running EPP tests. */
@Singleton
@Component(
    modules = {
        DonutsConfigModule.class,
        DonutsEppTestComponent.FakesAndMocksModule.class,
    }
)
public interface DonutsEppTestComponent {

  DonutsRequestComponent startRequest();

  /** Module for injecting fakes and mocks. */
  @Module
  class FakesAndMocksModule {

    final FakeClock clock;
    final Sleeper sleeper;
    final DnsQueue dnsQueue;
    final BigQueryMetricsEnqueuer metricsEnqueuer;
    final EppMetric.Builder metricBuilder;
    final ModulesService modulesService;

    FakesAndMocksModule(FakeClock clock) {
      this.clock = clock;
      this.sleeper = new FakeSleeper(clock);
      this.dnsQueue = DnsQueue.create();
      this.metricBuilder = EppMetric.builderForRequest("request-id-1", clock);
      this.modulesService = mock(ModulesService.class);
      this.metricsEnqueuer = mock(BigQueryMetricsEnqueuer.class);
    }

    @Provides
    Clock provideClock() {
      return clock;
    }

    @Provides
    Sleeper provideSleeper() {
      return sleeper;
    }

    @Provides
    DnsQueue provideDnsQueue() {
      return dnsQueue;
    }

    @Provides
    EppMetric.Builder provideMetrics() {
      return metricBuilder;
    }

    @Provides
    ModulesService provideModulesService() {
      return modulesService;
    }

    @Provides
    BigQueryMetricsEnqueuer provideBigQueryMetricsEnqueuer() {
      return metricsEnqueuer;
    }

    @Provides
    CustomLogicFactory provideCustomLogicFactory() {
      return new DonutsCustomLogicFactory();
    }
  }

  /** Subcomponent for request scoped injections. */
  @RequestScope
  @Subcomponent
  interface DonutsRequestComponent {
    EppController eppController();
    FlowComponent.Builder flowComponentBuilder();
  }
}
