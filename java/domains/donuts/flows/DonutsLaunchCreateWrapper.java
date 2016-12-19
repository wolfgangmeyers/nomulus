package domains.donuts.flows;

import com.google.common.collect.ImmutableList;

import java.util.List;

import javax.annotation.Nullable;

import google.registry.model.domain.launch.LaunchCreateExtension;
import google.registry.model.domain.launch.LaunchPhase;
import google.registry.model.smd.AbstractSignedMark;


public class DonutsLaunchCreateWrapper {

  @Nullable
  private final LaunchCreateExtension launchCreate;

  public DonutsLaunchCreateWrapper(@Nullable final LaunchCreateExtension launchCreate) {
    this.launchCreate = launchCreate;
  }

  /**
   * The user has explicitly set the launch create phase to 'dpml' indicating they are expecting
   * a dpml override.
   */
  public boolean isDpmlRegistration() {
    return launchCreate != null && LaunchPhase.DPML.equals(launchCreate.getPhase());
  }

  public List<AbstractSignedMark> getSignedMarks() {
    return launchCreate == null
        ? ImmutableList.<AbstractSignedMark>of()
        : launchCreate.getSignedMarks();
  }
}
