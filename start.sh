#!/bin/bash
#############################################
#
# run musicrest
#
# usage: start.sh
#
#############################################

java -Dconfig.file=conf/musicrest.conf -classpath target/scala-2.11/musicrest-2.11-assembly-1.1.7.jar org.bayswater.musicrest.Boot

echo "return: " $retcode

exit $retcode
