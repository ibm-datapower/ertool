#!/bin/bash

SCRIPT=$(readlink -f $0)
SCRIPTPATH=`dirname $SCRIPT`

OLD="[0-9]\{1,2\}"
NEW="0"
DPATH="./*"
FILES=($SCRIPTPATH/stats.log $SCRIPTPATH/visits.log)

TFILE="out.tmp.$$"

for f in "${FILES[@]}"
    do
      if [ -f $f -a -r $f ]; then
        TEMP=$(sed "s/$OLD/$NEW/g" "$f")
       echo "$TEMP" > $f
        #$TFILE && mv $TFILE "$f"
      else
        echo "Error: Cannot read $f"
      fi
   
   done
/bin/rm $TFILE 2> "/dev/null"

LOG="$SCRIPTPATH/my.log"

echo "" > $LOG

