package org.cougaar.tools.robustness.ma;

import org.cougaar.tools.robustness.threatalert.ThreatAlert;
import org.cougaar.tools.robustness.threatalert.DefaultThreatAlert;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.util.UID;

import java.util.Date;

/**
 */
public class HostLossThreatAlert extends DefaultThreatAlert {

  /**
   * Create a new HostLossThreatAlert.
   * @param source         ThreatAlert source
   * @param severityLevel  Severity level of alert
   * @param start          Time at which alert becomes active
   * @param duration       Duration of threat period (-1 == never expires)
   * @param uid            Unique identifier
   */
  public HostLossThreatAlert(MessageAddress source,
                             int severityLevel,
                             Date start,
                             long duration,
                             UID uid) {
    super(source, severityLevel, start, duration, uid);
  }

}