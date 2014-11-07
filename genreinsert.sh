#!/bin/bash
#############################################
#
# insert all genres (and rhythms) into mongo
#
# usage: genreinsert.sh dbname
#
#############################################

EXPECTED_ARGS=1

if [ $# -ne $EXPECTED_ARGS ]
then
  echo "Usage: `basename $0` {dbname}"
  exit $E_BADARGS
fi

dbname=$1

java -Dconfig.file=conf/musicrest.conf -classpath target/scala-2.10/musicrest-2.10-1.1.5.jar org.bayswater.musicrest.tools.GenreInsert $dbname

echo "abc return: " $retcode

exit $retcode
