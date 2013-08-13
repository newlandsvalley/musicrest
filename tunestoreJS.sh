#!/bin/bash
#############################################
#
# generate javascript for trad tune store
#
# usage: tunestoreJS.sh 
#
#############################################

EXPECTED_ARGS=1

if [ $# -ne $EXPECTED_ARGS ]
then
  echo "Usage: `basename $0` {dbname}"
  exit $E_BADARGS
fi

dbname=$1

java -Dconfig.file=conf/musicrest.conf -classpath target/scala-2.10/musicrest-2.10.2.jar org.bayswater.musicrest.tools.TuneStoreJS $dbname


exit $retcode
