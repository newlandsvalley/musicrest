#!/bin/bash
#############################################
#
# transcode abc to pdf format
#
# usage: abc2pdf.sh srcdir destdir tunename
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

# transcode from .abc to .eps (-E generates a 001 extension to the name)
abcm2ps -E $abcdir/$3.abc -O $workdir/$3.eps
retcode=$?
echo "abc return: " $retcode


# transcode from .eps to .pdf
if [ $retcode -eq 0 ]; then
  echo "attempting to transcode ps to pdf"
  convert -auto-orient $workdir/$3001.eps $pdfdir/$3.pdf
  retcode=$?
fi

# remove the intermediate .eps file
rm -f $workdir/$3001.eps

exit $retcode
