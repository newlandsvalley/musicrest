#!/bin/bash
#############################################
#
# transcode abc to ps format
#
# usage: abc2ps.sh srcdir destdir tunename
#
#############################################

EXPECTED_ARGS=3

if [ $# -ne $EXPECTED_ARGS ]
then
  echo "Usage: `basename $0` {srcdir destdir tunename}"
  exit $E_BADARGS
fi

# source
abcdir=$1
if [ ! -d $abcdir ]
then
  echo "$abcdir not a directory" >&2   # Error message to stderr.
  exit 1
fi  

# destination
pdfdir=$2
if [ ! -d $pdfdir ]
then
  echo "$pdfdir not a directory" >&2   
  exit 1
fi  

# temporary work directory (we'll reuse src for the time being)
workdir=$1

# source file
srcfile=${abcdir}/${3}.abc
if [ ! -f $srcfile ]
then
  echo "no such file $srcfile" >&2  
  exit 1
fi  

# transcode from .abc to .ps
abcm2ps $abcdir/$3.abc -O $pdfdir/$3.ps
retcode=$?
echo "abc return: " $retcode

exit $retcode
