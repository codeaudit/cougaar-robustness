This folder contains patches to other modules that were requested per bug reports in order to allow acuc2b to work, but that were never completed by the other teams due to the end of the program.  In order for acuc2b to work, the following needs to be done:

- replace $CIP/configs/coordinator/DOS/DDoS_Actuators.xml with the equivalent file from this directory
- add the ./security subdirectory to $CIP/configs/security and run the "sh $CIP/operator/updateSecurityTechspecs.sh".  This will replace and bad security TS's and jar them
- $CIP/opereator/createJarConfigFiles to jar all config files
- replace the existing $CIP/lib/secure-coordinator.jar with the equivalent from this directory