#!/bin/bash
#############################################
#
# bulk export all abc from mongo genre
#
# usage: bulkexport.sh abcdir dbname genre
#
#############################################

EXPECTED_ARGS=3

if [ $# -ne $EXPECTED_ARGS ]
then
  echo "Usage: `basename $0` {abcdir dbname genre}"
  exit $E_BADARGS
fi

# source
abcdir=$1
if [ ! -d $abcdir ]
then
  echo "$abcdir not a directory" >&2   # Error message to stderr.
  exit 1
fi  

dbname=$2
genre=$3

java -classpath target/scala-2.10/musicrest-2.10-1.1.4.jar org.bayswater.musicrest.tools.BulkExport $abcdir $dbname $genre

echo "abc return: " $retcode

exit $retcode
