#!/bin/sh

# Wrapper around the Cougaar 'Node' program that will set things up
# for running in the Telcordia Coordinator environment.  Simply pass
# in the name of the node you want to run (with or without the .ini
# extension).

# Check command line stuff.
#
if [ $# -ne 1 ] ; then
  echo "Usage: $0 [node-name]"
  exit 1
fi

# Strip trailing ".ini" suffix if it exists.
#
node_name=`echo $1 | sed s/\\\\.ini\\\$//`

# Because the test directory might be relative to where we need to
# build things, we should immediately save this directory, so that we
# can return here after compiling. 

test_dir=`pwd`

# Find the  directory where the environment variable setting file
# lives. Must be an ancestor of this directory. 
#
config_dir=`pwd`
for n in 1 2 3 4 5 6 7 8 9  ; do
  if [ -r $config_dir/$USER-ul-env.sh ] ; then
    break
  fi
  if [ $n -eq 9 ] ; then
    echo "Cannot find environment settings file: $USER-ul-env.sh. Aborting."
    exit 1
  fi
  config_dir="$config_dir/.."
done

cd $config_dir
config_dir=`pwd`
echo "FOUND: Config file: $config_dir/$USER-ul-env.sh"

# Source these environment variables
#
. $config_dir/$USER-ul-env.sh

status=0

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

# Find the  directory where the ant build file
# lives. Must be an ancestor directory. 
#
cd $test_dir
compile_dir=`pwd`
for n in 1 2 3 4 5 6 7 8 9  ; do
  if [ -r $compile_dir/build.xml ] ; then
    break
  fi
  if [ $n -eq 9 ] ; then
    echo "Cannot find ant build file: build.xml. Aborting."
    exit 1
  fi
  compile_dir="$compile_dir/.."
done

cd $compile_dir
compile_dir=`pwd`
echo "FOUND: Build file: $compile_dir/build.xml"

# Make sure we have compiled the code using ant:
#
echo "PWD: "`pwd`
echo "EXEC: $ANT compile"
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

cmd="$COUGAAR_INSTALL_PATH/bin/Node \
    -Dorg.cougaar.core.logging.config.filename=logger.cfg \
    -Dorg.cougaar.class.path=$compile_dir/build \
    -Dorg.cougaar.core.persistence.enable=false \
    -Dorg.cougaar.core.persistence.clear=true \
    -Dorg.cougaar.config.path=\
./\
;$OBJS_CODE_BASE/../examples/acuc1\
;$COUGAAR_INSTALL_PATH/configs/common\
;$COUGAAR_INSTALL_PATH/configs/glmtrans\
;$OBJS_CODE_BASE/../configs\
;$OBJS_CODE_BASE/../configs/coordinator\
;$OBJS_CODE_BASE/../configs/coordinator/SampleDefense\
;$OBJS_CODE_BASE/../configs/coordinator/LeashDefenses\
;$OBJS_CODE_BASE/../configs/coordinator/Sledgehammer\
;$OBJS_CODE_BASE/../configs/coordinator/MsgLog\
;$OBJS_CODE_BASE/../configs/coordinator/PlannedDisconnect\
;$OBJS_CODE_BASE/../configs/coordinator/DDoS\
    $node_name"

echo ""
echo "CMD: $cmd"
echo ""

$cmd
