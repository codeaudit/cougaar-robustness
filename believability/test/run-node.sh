#!/bin/sh

# Wrapper around the Cougaar 'Node' program that will set things up
# for running in the Telcordia Coordinator environment.  Simply pass
# in the name of the node you want to run.

# Check command line stuff.
#
if [ $# -ne 1 ] ; then
  echo "Usage: $0 [node-name]"
  exit 1
fi

# See where the test directory is (assume other test files in same
# directory as this script. 
#
test_dir=`dirname $0`

# because the test directory might be relative, we should immediately
# change to it, and then push it onto the directory stack, so that we
# can return here after compiling.
#
cd $test_dir

test_dir=`pwd`

# Strip off trailing ".ini" if any.
#
node_name=`echo $1 | sed s/\\\\.ini\\\$//`

# First set up environment variables, though skip this if someone set
# UL_ENV_SKIP
#
if [ "x$UL_ENV_SKIP" != "xtrue" ] ; then

  echo "NOTE: Getting environment from : $test_dir/../$USER-ul-env.sh"
  . $test_dir/../$USER-ul-env.sh

  BUILD_DIR=$TELCORDIA_CODE_BASE/../build
  echo "NOTE: Using build in: $BUILD_DIR"
 
else
  echo "NOTE: Skipped default environment variable settings."
fi

status=0
if [ "x$BUILD_DIR" = "x" ] ; then
  echo "!!! No BUILD_DIR environment variable set."
  status=1
fi

if [ "x$COUGAAR_INSTALL_PATH" = "x" ] ; then
  echo "!!! No COUGAAR_INSTALL_PATH environment variable set."
  status=1
fi

if [ "x$TELCORDIA_CODE_BASE" = "x" ] ; then
  echo "!!! No TELCORDIA_CODE_BASE environment variable set."
  status=1
fi

if [ "x$OBJS_CODE_BASE" = "x" ] ; then
  echo "!!! No OBJS_CODE_BASE environment variable set."
  status=1
fi

if [ $status -ne 0 ] ; then
  echo "Aborted."
  exit $status
fi

which ant > /dev/null
if [ $? -eq 0 ] ; then
  ANT=`which ant`
else
  if [ ! -r /u/arcshare/Ultralog/apps/ant/latest/bin/ant ] ; then
    echo "Canot find 'ant' executable."
    exit 1
  fi
  ANT=/u/arcshare/Ultralog/apps/ant/latest/bin/ant
fi

# Make sure we have compiled the code using ant:
#
cd $TELCORDIA_CODE_BASE/..
$ANT compile
if [ $? -ne 0 ] ; then
  echo "Build error.  Fix code and please try again."
  echo "Aborted."
  exit 1
fi

# Return to test directory before executing cougaar node
#
cd $test_dir

#export CLASSPATH=$TELCORDIA_CODE_BASE:$OBJS_CODE_BASE

$COUGAAR_INSTALL_PATH/bin/Node \
    -Dorg.cougaar.core.logging.config.filename=logger.cfg \
    -Dorg.cougaar.class.path=$BUILD_DIR \
    -Dorg.cougaar.core.persistence.enable=false \
    -Dorg.cougaar.core.persistence.clear=true \
    $node_name
