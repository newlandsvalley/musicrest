#!/bin/bash
################################################
#
# insert all genres (and rhythms) into mongo
#
# usage: genreinsert.sh dbuser dbpassword dbname
#
#################################################

EXPECTED_ARGS=4

if [ $# -ne $EXPECTED_ARGS ]
then
  echo "Usage: `basename $0` {dbhost} {dbuser} {dbpassword} {dbname}"
  exit $E_BADARGS
fi

dbhost=$1
dbuser=$2
dbpassword=$3
dbname=$4

java -Dconfig.file=conf/musicrest.conf -classpath target/scala-2.11/musicrest-2.11-assembly-1.3.0.jar org.bayswater.musicrest.tools.GenreInsert $dbhost $dbuser $dbpassword $dbname

echo "abc return: " $retcode

exit $retcode
