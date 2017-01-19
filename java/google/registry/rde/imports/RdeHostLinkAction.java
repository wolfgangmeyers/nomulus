package google.registry.rde.imports;

import static com.google.common.base.Preconditions.checkState;
import static google.registry.mapreduce.MapreduceRunner.PARAM_MAP_SHARDS;
import static google.registry.model.EppResourceUtils.loadByForeignKey;
import static google.registry.model.ofy.ObjectifyService.ofy;
import static google.registry.model.registry.Registries.findTldForName;
import static google.registry.util.PipelineUtils.createJobPath;

import com.google.appengine.tools.mapreduce.Mapper;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.net.InternetDomainName;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.VoidWork;
import google.registry.config.RegistryConfig.Config;
import google.registry.mapreduce.MapreduceRunner;
import google.registry.model.domain.DomainResource;
import google.registry.model.host.HostResource;
import google.registry.request.Action;
import google.registry.request.Parameter;
import google.registry.request.Response;
import google.registry.util.FormattingLogger;
import google.registry.xjc.JaxbFragment;
import google.registry.xjc.rdehost.XjcRdeHost;
import google.registry.xjc.rdehost.XjcRdeHostElement;
import org.joda.time.DateTime;

import javax.inject.Inject;

/**
 * A mapreduce that links hosts from an escrow file to their superordinate domains.
 *
 * <p>Specify the escrow file to import with the "path" parameter.
 */
@Action(path = "/_dr/task/linkRdeHosts")
public class RdeHostLinkAction implements Runnable {
  private static final FormattingLogger logger = FormattingLogger.getLoggerForCallerClass();

  private final MapreduceRunner mrRunner;
  private final Response response;
  private final String importBucketName;
  private final String importFileName;
  private final Optional<Integer> mapShards;

  @Inject
  public RdeHostLinkAction(
      MapreduceRunner mrRunner,
      Response response,
      @Config("rdeImportBucket") String importBucketName,
      @Parameter("path") String importFileName,
      @Parameter(PARAM_MAP_SHARDS) Optional<Integer> mapShards) {
    this.mrRunner = mrRunner;
    this.response = response;
    this.importBucketName = importBucketName;
    this.importFileName = importFileName;
    this.mapShards = mapShards;
  }

  @Override
  public void run() {
    response.sendJavaScriptRedirect(createJobPath(mrRunner
        .setJobName("Link hosts from escrow file")
        .setModuleName("backend")
        .runMapOnly(
            new RdeHostPostImportMapper(),
            ImmutableList.of(new RdeHostInput(mapShards, importBucketName, importFileName)))));
  }

  /**
   * Return the {@link DomainResource} this host is subordinate to, or null for out of zone hosts.
   *
   * @throws {@link IllegalStateException} for hosts without superordinate domains
   */
  private static DomainResource lookupSuperordinateDomain(InternetDomainName hostName, DateTime now) {
    Optional<InternetDomainName> tld = findTldForName(hostName);
    // out of zone hosts cannot be linked
    if(!tld.isPresent()) {
      return null;
    }
    // This is a subordinate host
    String domainName = Joiner.on('.').join(Iterables.skip(
        hostName.parts(), hostName.parts().size() - (tld.get().parts().size() + 1)));
    DomainResource superordinateDomain = loadByForeignKey(DomainResource.class, domainName, now);
    // Hosts can't be linked if domains import hasn't been run
    checkState(superordinateDomain != null,
      "Superordinate domain not found: %s",
      domainName);
    return superordinateDomain;
  }

  /** Mapper to link hosts from an escrow file to their superordinate domains. */
  public static class RdeHostPostImportMapper extends Mapper<JaxbFragment<XjcRdeHostElement>, Void, Void> {

    private static final long serialVersionUID = -2898753709127134419L;

    @Override
    public void map(JaxbFragment<XjcRdeHostElement> fragment) {
      // Record number of attempted map operations
      getContext().incrementCounter("post-import hosts read");
      final XjcRdeHost xjcHost = fragment.getInstance().getValue();
      logger.infofmt("Attempting to link superordinate domain for host %s", xjcHost.getName());
      try {
        InternetDomainName hostName = InternetDomainName.from(xjcHost.getName());
        DomainResource superordinateDomain = lookupSuperordinateDomain(hostName, DateTime.now());
        // if suporordinateDomain is null, this is an out of zone host and can't be linked
        if (superordinateDomain == null) {
          getContext().incrementCounter("post-import hosts out of zone");
          logger.infofmt("Host %s is out of zone", xjcHost.getName());
          return;
        }
        // at this point, the host is definitely in zone and should be linked
        getContext().incrementCounter("post-import hosts in zone");
        final Key<DomainResource> superordinateDomainKey = Key.create(superordinateDomain);
        ofy().transact(new VoidWork() {
          @Override
          public void vrun() {
            HostResource host =
                ofy().load().now(Key.create(HostResource.class, xjcHost.getRoid()));
            ofy().save()
                .entity(host.asBuilder().setSuperordinateDomain(superordinateDomainKey).build());
          }
        });
        logger.infofmt(
            "Successfully linked host %s to superordinate domain %s",
            xjcHost.getName(),
            superordinateDomain.getFullyQualifiedDomainName());
        // Record number of hosts successfully linked
        getContext().incrementCounter("post-import hosts linked");
      } catch (Exception e) {
        // Record the number of hosts with unexpected errors
        getContext().incrementCounter("post-import host errors");
        throw new HostLinkException(xjcHost.getName(), xjcHost.toString(), e);
      }
    }
  }

  private static class HostLinkException extends RuntimeException {
    HostLinkException(String hostname, String xml, Throwable cause) {
      super(String.format("Error linking host %s; xml=%s", hostname, xml), cause);
    }
  }
}
