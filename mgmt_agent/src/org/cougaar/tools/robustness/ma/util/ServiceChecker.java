package org.cougaar.tools.robustness.ma.util;

import org.cougaar.core.service.LoggingService;

import org.cougaar.tools.robustness.ma.util.PingHelper;
import org.cougaar.tools.robustness.ma.util.PingListener;
import org.cougaar.tools.robustness.ma.util.PingResult;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.service.community.CommunityService;
import org.cougaar.core.service.community.Community;
import org.cougaar.core.service.community.CommunityResponseListener;
import org.cougaar.core.service.community.CommunityResponse;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.Attribute;

/**
 * Helper class used to verify the availability of essential service providers.
 * This class is nominally used by the Adaptive Robustness restart defense to
 * ensure that certain providers (such as a Certificate Authority) are available
 * prior to initiating a restart.  The identification of service groups and
 * providers is defined using community attributes.  Provider availability is
 * determined by pinging the provider agent(s).
 */

public class ServiceChecker {

  protected ServiceBroker serviceBroker;
  protected PingHelper pingHelper;
  protected CommunityService communityService;
  protected LoggingService logger;

  protected Map serviceMaps = Collections.synchronizedMap(new HashMap());

  public ServiceChecker(ServiceBroker sb, PingHelper ph) {
    this.serviceBroker = sb;
    this.pingHelper = ph;
    logger =
      (LoggingService)serviceBroker.getService(this, LoggingService.class, null);
  }

  protected CommunityService getCommunityService() {
    if (communityService == null) {
      communityService =
      (CommunityService)serviceBroker.getService(this, CommunityService.class, null);
    }
    return communityService;
  }

  /**
   * Check the availability of service providers for all services defined
   * by specified service category.  The services and providers are defined
   * in by attributes in the specified community.
   * @param communityName String      Name of community containing provider attributes
   * @param serviceCategory String    Name of service category
   * @param timeout int               Ping duration
   * @param csl CheckServicesListener Client callback
   */
  public void checkServices(final String                communityName,
                            final String                serviceCategory,
                            final long                  timeout,
                            final CheckServicesListener csl) {
    if (logger.isDebugEnabled()) {
      logger.debug("checkServices:" +
                   " communityName=" + communityName +
                   " serviceCategory=" + serviceCategory +
                   " timeout=" + timeout);
    }
    if (csl != null) {
      if (communityName == null || serviceCategory == null) {
        csl.execute(communityName, serviceCategory, false, "Null community or category");
      }
      CommunityService cs = getCommunityService();
      if (cs != null) {
        Community community = cs.getCommunity(communityName,
                                              new CommunityResponseListener() {
          public void getResponse(CommunityResponse resp) {
            Community community = (Community) resp.getContent();
            if (community != null) {
              checkServices(community, serviceCategory, timeout, csl);
            } else {
              csl.execute(communityName, serviceCategory, false, "Null community");
            }
          }
        });
        if (community != null) {
          checkServices(community, serviceCategory, timeout, csl);
        }
      }
    }
  }

  protected void checkServices(Community             community,
                               String                serviceCategory,
                               long                  timeout,
                               CheckServicesListener csl) {
    Attributes attrs = community.getAttributes();
    Set services = getAttributeValues(attrs, serviceCategory);
    if (services.isEmpty()) {
      csl.execute(community.getName(), serviceCategory, true, "No essential services defined");
    } else {
      Set agentSet = new HashSet();
      Map providerMap = new HashMap();
      for (Iterator it = services.iterator(); it.hasNext(); ) {
        String serviceName = (String)it.next();
        Set providerNames =
            getAttributeValues(attrs, serviceName + "-ServiceProvider");
        Set providerStatus = new HashSet();
        for (Iterator it1 = providerNames.iterator(); it1.hasNext();) {
          providerStatus.add(new ProviderEntry((String)it1.next(), PingResult.UNDEFINED));
        }
        providerMap.put(serviceName, providerStatus);
        ServiceEntry se = (ServiceEntry)serviceMaps.get(serviceCategory);
        if (se != null) {
          se.listeners.add(csl);
        } else {
          serviceMaps.put(serviceCategory, new ServiceEntry(community.getName(),
              serviceCategory,
              providerMap,
              csl));
        }
        agentSet.addAll(providerNames);
      }
      if (logger.isDebugEnabled()) {
        logger.debug("checkServices:" +
                     " communityName=" + community.getName() +
                     " serviceCategory=" + serviceCategory +
                     " timeout=" + timeout +
                     " services=" + providerMap.keySet() +
                     " providers={" + listProviders(providerMap) + "}");
      }
      PingListener pl = new PingListener() {
        public void pingComplete(PingResult[] result) {
          for (int i = 0; i < result.length; i++) {
            if (logger.isDebugEnabled()) {
                logger.debug("pingResult:" +
                            " agent=" + result[i].getName() +
                            " result=" + result[i].statusAsString(result[i].getStatus()));
            }
            processPingResponse(result[i].getName(), result[i].getStatus());
          }
        }
      };
      for (Iterator it = agentSet.iterator(); it.hasNext();) {
        String agentName = (String)it.next();
        if (!pingHelper.pingInProcess(agentName)) {
          if (logger.isDebugEnabled()) {
            logger.debug("pinging agent: agent=" + agentName + " timeout=" +
                          timeout);
          }
          pingHelper.ping(new String[] {agentName}, timeout, pl);
        }
      }

    }
  }

  protected void processPingResponse(String agentName, int status) {
    try {
      Collection serviceEntries = serviceMaps.values();
      for (Iterator it = serviceEntries.iterator(); it.hasNext(); ) {
        ServiceEntry se = (ServiceEntry) it.next();
        for (Iterator it1 = se.providers.values().iterator(); it1.hasNext(); ) {
          Set providerEntries = (Set) it1.next();
          for (Iterator it2 = providerEntries.iterator(); it2.hasNext(); ) {
            ProviderEntry pe = (ProviderEntry) it2.next();
            if (pe.provider.equals(agentName)) {
              pe.status = status;
            }
          }
        }
        checkAvailability(se);
      }
    } catch (Exception ex) {
      logger.error(ex.getMessage(), ex);
    }
  }

  protected void checkAvailability(ServiceEntry se) {
    boolean avail = true;
    boolean complete = false;
    String message = "";
    Map providerEntries = se.providers;
    Iterator it = providerEntries.entrySet().iterator();
    while ((avail || !complete) && it.hasNext()) {
      avail = false;
      complete = true;
      Map.Entry me = (Map.Entry)it.next();
      String service = (String)me.getKey();
      Set providers = (Set)me.getValue();
      for (Iterator it1 = providers.iterator(); it1.hasNext(); ) {
        ProviderEntry pe = (ProviderEntry) it1.next();
        if (pe.status == PingResult.SUCCESS) {
          avail = true;
          break;
        } else if (pe.status == PingResult.UNDEFINED) {
          complete = false;
        }
        if (complete) {
          message = "No provider found for service '" + service + "'";
        }
      }
    }
    if (avail || complete) {
      serviceMaps.remove(se.category);
      for (Iterator it1 = se.listeners.iterator(); it1.hasNext();) {
        CheckServicesListener csl = (CheckServicesListener)it1.next();
        csl.execute(se.cname, se.category, avail, message);
      }
    }
  }

  protected Set getAttributeValues(Attributes attrs, String id) {
    Set values = new HashSet();
    Attribute attr = attrs.get(id);
    if (attr != null) {
      try {
        NamingEnumeration enum = attr.getAll();
        while (enum.hasMore()) {
          values.add(enum.next());
        }
      } catch (NamingException ne) {
      }
    }
    return values;
  }

  protected String listProviders(Map serviceProviderMap) {
    StringBuffer sb = new StringBuffer();
    for (Iterator it = serviceProviderMap.entrySet().iterator(); it.hasNext();) {
      Map.Entry me = (Map.Entry)it.next();
      String serviceName = (String)me.getKey();
      Set providers = new HashSet();
      Set statusEntries = (Set)me.getValue();
      for (Iterator it1 = statusEntries.iterator(); it1.hasNext();) {
        ProviderEntry pe = (ProviderEntry)it1.next();
        providers.add(pe.provider);
      }
      sb.append(serviceName + "=" + providers);
      if (it.hasNext()) {
        sb.append(", ");
      }
    }
    return sb.toString();
  }

  class ServiceEntry {
    String cname;
    String category;
    Map providers;
    List listeners;
    ServiceEntry (String comm, String cat, Map p, CheckServicesListener l) {
      cname = comm;
      category = cat;
      providers = p;
      listeners = new ArrayList();
      listeners.add(l);
    }
  }

  class ProviderEntry {
    String provider;
    int status;
    ProviderEntry (String p, int s) {
      provider = p;
      status = s;
    }
  }

}
