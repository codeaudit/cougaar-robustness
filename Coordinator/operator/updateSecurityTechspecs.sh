#!/bin/sh

rm -rf $CIP/workspace/jarfiles
mkdir $CIP/workspace/jarfiles
rm -f $CIP/configs/security/configs_secure-coordinator.jar
jar cf $CIP/configs/security/configs_secure-coordinator.jar $CIP/configs/security/security
$CIP/operator/sign $CIP/configs/security/configs_secure-coordinator.jar

