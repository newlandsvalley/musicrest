#!/bin/sh
#############################################
#
# run musicrest
#
# usage: musicrest.sh
#
#############################################

# this changes to the parent directory.  why?
cd $(dirname $0)/../

exec java -Dconfig.file=conf/musicrest.conf -classpath target/scala-2.10/musicrest-2.10.2.jar org.bayswater.musicrest.Boot

exit $retcode
