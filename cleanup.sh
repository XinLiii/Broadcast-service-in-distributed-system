#!/bin/bash

NETID=`whoami`
PROJDIR=`pwd`
# Directory where the config file is located on your local system
CONFIGFILE=$PROJDIR/config3
python convert_config.py $CONFIGFILE $CONFIGFILE

# parse config file and end nodes
n=0
# remove comment and remove whitespace lines
cat $CONFIGFILE | sed -e "s/#.*//" | sed -e "/^\s*$/d" |
(
    read i
    echo $i
    while [[ $n -lt $i ]]
    do
    	read line
		echo $line
        host=$( echo $line | awk '{ print $2 }' )
        ssh $NETID@$host "killall -u $NETID;" &
        sleep 0.1
        n=$(( n + 1 ))
    done
)
echo "Cleanup complete"
