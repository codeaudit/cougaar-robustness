

Summary
-------
This tests the deconfliction module. Defense Condition names and values
are set via the servlet. This should cause corresponding defense operating 
modes to be changed, per the playbook. The test is successful if when a
specified defense condition is set, that the corresponding operating modes 
are changed.

Requirements (two nodes)
------------

  In node 1 - named AE
  ---------

  plugin = org.cougaar.core.adaptivity.OperatingModeServiceProvider
  plugin = org.cougaar.core.adaptivity.OperatingModePolicyManager
  plugin = org.cougaar.core.adaptivity.AdaptivityEngine
  plugin = org.cougaar.core.adaptivity.PlaybookManager(test_playbook.txt)
  plugin = org.cougaar.core.adaptivity.ConditionServiceProvider

  Inclusion of test_playbook.txt


  In node 2 - named Defense
  ---------
  plugin = org.cougaar.core.adaptivity.OperatingModeServiceProvider
  plugin = org.cougaar.core.adaptivity.OperatingModePolicyManager
  plugin = org.cougaar.core.adaptivity.PlaybookManager(test_playbook.txt)
  plugin = org.cougaar.core.adaptivity.ConditionServiceProvider

  plugin = org.cougaar.tools.robustness.deconfliction.RemoteDefenseConditionMgrPlugin(AE)

  plugin = org.cougaar.tools.robustness.deconfliction.test.defense.DeconflictionServlet
  plugin = org.cougaar.tools.robustness.deconfliction.test.defense.MyServletTestDefense
  plugin = org.cougaar.tools.robustness.deconfliction.test.defense.DefenseOperatingModeChangeEvents




Test Description
----------------
Using the provided servlet, set a condition "condName" to TRUE or FALSE. When 
this happens an EventService Event will be generated, of the form:

	"Condition <condName> set to <value>"
     

Once the condition is set to TRUE, if a corresponding rule in the playbook exists,
then affected operating modes are changed. Changed Defense Operating Modes are
emitted as Events in the form:

DefenseOperatingMode: <defense operating mode name>=<value>

Test Case #1
------------

Set MyDefense.MyCondition to TRUE, you should see the Event:
($AE/Deconfliction?condName=MyDefense.MyCondition&condValue=TRUE&Submit=Submit)

	Condition MyDefense.MyCondition set to TRUE       

Once the condition is set to TRUE, two Events should be emitted:

	DefenseOperatingMode: MyDefense.MyEnabler=DISABLED
	DefenseOperatingMode: MyDefense.MyMonitor=DISABLED


Test Case #2
------------
Set MyDefense.MyCondition to FALSE, you should see the Event:
($AE/Deconfliction?condName=MyDefense.MyCondition&condValue=FALSE&Submit=Submit)

	Condition MyDefense.MyCondition set to FALSE       

Once the condition is set to TRUE, two Events should be emitted:

	DefenseOperatingMode: MyDefense.MyEnabler=PREPARE
	DefenseOperatingMode: MyDefense.MyMonitor=ENABLED







