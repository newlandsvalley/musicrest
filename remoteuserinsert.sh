#!/bin/bash
##################################################################
#
# insert a user into Musicrest via the REST API
#
# usage: remoteuserinsert.sh server adminpass uname password email
#
##################################################################

EXPECTED_ARGS=5

if [ $# -ne $EXPECTED_ARGS ]
then
  echo "Usage: `basename $0` {server} {adminpass} {uname} {password} {email}"
  exit $E_BADARGS
fi

server=$1
adminpass=$2
uname=$3
password=$4
email=$5

java -Dconfig.file=conf/musicrest.conf -classpath target/scala-2.10/musicrest-2.10-1.1.5.jar org.bayswater.musicrest.tools.RemoteUserInsert $server $adminpass $uname $password $email

exit $retcode
