#!/bin/bash
#############################################
#
# run musicrest
#
# usage: start.sh
#
#############################################

java -Dconfig.file=conf/musicrest.conf -classpath target/scala-2.10/musicrest-2.10-1.1.6.jar org.bayswater.musicrest.Boot

echo "return: " $retcode

exit $retcode
