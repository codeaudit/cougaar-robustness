
This tests the disconnection defense using the deconfliction API.
Before running the test (config/script/robustness/uc7/Run_UC7_SocC.rb)
you need to copy the file NULL_playbook.txt to $CIP/configs/common.
You will need to run dos2unix on this file. The workspace/Log4JLogs 
log files will contain errors if this is not done correctly (essentially 
all we need is an empty file).

Now it should be safe to run the test. See ./TestProcedure.htm for more
general information on this test.