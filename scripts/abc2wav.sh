#!/bin/bash
#############################################
#
# transcode abc to wav format
#
# usage: abc2wav.sh srcdir destdir tunename
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

# temporary work directory (we'll reuse dest for the time being)
workdir=$2

# source file
srcfile=${abcdir}/${3}.abc
if [ ! -f $srcfile ]
then
  echo "no such file $srcfile" >&2  
  exit 1
fi  

# transcode from .abc to .midi
abc2midi $abcdir/$3.abc -o $pdfdir/$3.midi
retcode=$?
echo "abc return: " $retcode

# transcode from .midi to .wav
if [ $retcode -eq 0 ]; then
  echo "attempting to transcode midi to wav"
  timidity -Ow $pdfdir/$3.midi -o $pdfdir/$3.wav
  retcode=$?
  echo "timidity return: " $retcode
fi

# remove the intermediate .midi file
# rm -f $workdir/$3.midi

exit $retcode
