/*
 * <copyright>
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 *
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */
package org.cougaar.tools.robustness.ma.plugins;

import java.util.Properties;

/**
 * Defines the names and values of configurable parameters used by various
 * Management Agent plugins.
 */
public class ManagementAgentProperties extends Properties
  implements java.io.Serializable {

  // Defines the name of the plugin that this Properties object is used by.
  private String pluginName = new String();

  /**
   * Constructor that defines the intended user of these properties.
   * @param pluginName  Name of plugin
   */
  public ManagementAgentProperties(String pluginName) {
    super();
    this.pluginName = pluginName;
  }

  /**
   * Constructor that defines the intended user of these properties and
   * default properties.
   * @param pluginName  Name of plugin
   * @param defaults    Default properties
   */
  public ManagementAgentProperties(String pluginName, Properties defaults) {
    super(defaults);
    this.pluginName = pluginName;
  }

  /**
   * Get name of using plugin.
   * @return Plugin name
   */
  public String getPluginName() {
    return pluginName;
  }

  /**
   * Set name of using plugin.
   * @param pluginName
   */
  public void setPluginName(String pluginName) {
    this.pluginName = pluginName;
  }

  /**
   * Utility method that creates a Properties object from a two dimension
   * String array.
   * @param name Components name
   * @param s  Two-dimension array of parameters name/value pairs
   * @return   Properties object with parameter name/values
   */
  public static ManagementAgentProperties makeProps(String name, String[][] s) {
    ManagementAgentProperties props =
      new ManagementAgentProperties(name);
    for (int i = 0; i < s.length; i++)
      if (s[i].length == 2)
        props.setProperty(s[i][0], s[i][1]);
    return props;
  }

}