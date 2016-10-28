#!/bin/sh
#############################################
#
# run musicrest
#
# usage: musicrest.sh
#
#############################################

exec java -Dconfig.file=conf/musicrest.conf -classpath target/scala-2.11/musicrest-2.11-assembly-1.1.7.jar org.bayswater.musicrest.Boot

exit $retcode
