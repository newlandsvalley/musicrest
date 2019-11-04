#!/bin/bash
##################################################################
#
# bulk export all abc from mongo genre
#
# usage: bulkexport.sh genre abcdir
#
###################################################################

EXPECTED_ARGS=2

if [ $# -ne $EXPECTED_ARGS ]
then
  echo "Usage: `basename $0` {collection} {abcdir}"
  exit $E_BADARGS
fi


collection=$1
# source
abcdir=$2
if [ ! -d $abcdir ]
then
  echo "$abcdir not a directory" >&2   # Error message to stderr.
  exit 1
fi


java -Dconfig.file=conf/musicrest.conf -classpath target/scala-2.11/musicrest-2.11-assembly-1.3.0.jar org.bayswater.musicrest.tools.BulkExport $collection $abcdir

echo "abc return: " $retcode

exit $retcode
