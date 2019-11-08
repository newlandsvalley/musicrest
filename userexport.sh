#!/bin/bash
##################################################################
#
# export the user list from Musicrest via the REST API
#
# usage: remoteusrexport.sh dirpath
#
##################################################################

EXPECTED_ARGS=1

if [ $# -ne $EXPECTED_ARGS ]
then
  echo "Usage: `basename $0` {dir path}"
  exit $E_BADARGS
fi


dir=$1

java -Dconfig.file=conf/musicrest.conf -classpath target/scala-2.11/musicrest-2.11-assembly-1.3.0.jar org.bayswater.musicrest.tools.UserExport $dir
exit $retcode
