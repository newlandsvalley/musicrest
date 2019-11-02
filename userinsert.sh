#!/bin/bash
##########################################################################
#
# insert a user into Mongo
# (specifically bootstrap by adding administrator)
#
# usage: userinsert.sh dbhost dbuser dbpassword dbname uname password email
#
###########################################################################

EXPECTED_ARGS=7

if [ $# -ne $EXPECTED_ARGS ]
then
  echo "Usage: `basename $0` {dbhost} {dbuser} {dbpassword} {dbname} {uname} {password} {email}"
  exit $E_BADARGS
fi


dbhost=$1
dbuser=$2
dbpassword=$3
dbname=$4
uname=$5
password=$6
email=$4

java -Dconfig.file=conf/musicrest.conf -classpath target/scala-2.11/musicrest-2.11-assembly-1.3.0.jar org.bayswater.musicrest.tools.UserInsert $dbhost $dbuser $dbpassword $dbname $uname $password $email


echo "abc return: " $retcode

exit $retcode
