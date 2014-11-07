#!/bin/bash
#############################################
#
# bulk import all abc files into mongo
#
# usage: bulkimport.sh abcdir dbname collection
#
#############################################

EXPECTED_ARGS=3

if [ $# -ne $EXPECTED_ARGS ]
then
  echo "Usage: `basename $0` {abcdir dbname collection}"
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
collection=$3

java -Dconfig.file=conf/musicrest.conf -classpath target/scala-2.10/musicrest-2.10-1.1.5.jar org.bayswater.musicrest.tools.BulkImport $abcdir $dbname $collection

echo "abc return: " $retcode

exit $retcode
