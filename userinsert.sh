#!/bin/bash
#############################################
#
# insert a user into Mongo
# (specifically bootstrap by adding administrator)
#
# usage: userinsert.sh dbname uname password email
#
#############################################

EXPECTED_ARGS=4

if [ $# -ne $EXPECTED_ARGS ]
then
  echo "Usage: `basename $0` {dbname} {uname} {password} {email}"
  exit $E_BADARGS
fi

dbname=$1
uname=$2
password=$3
email=$4

java -Dconfig.file=conf/musicrest.conf -classpath target/scala-2.11/musicrest-2.11-assembly-1.1.7.jar org.bayswater.musicrest.tools.UserInsert $dbname $uname $password $email


echo "abc return: " $retcode

exit $retcode
