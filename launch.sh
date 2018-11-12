#!/bin/bash

# Change this to your netid
NETID=`whoami`

# Root directory of your project
PROJDIR=`pwd`

# Directory where the config file is located on your local system
CONFIGFILE=$PROJDIR/config3
python convert_config.py $CONFIGFILE $CONFIGFILE

# Directory your java classes are in
BINARYDIR=$PROJDIR/bin
if [ ! -d "$BINARYDIR" ]; then
  mkdir $BINARYDIR
fi

# output dir
OUTPUTDIR=$PROJDIR/out
if [ ! -d "$OUTPUTDIR" ]; then
  mkdir $OUTPUTDIR
fi

# Your main project class
PROGRAM=BroadcastService

# compile java file
javac -d $BINARYDIR/ $PROGRAM.java
echo 'java compilation done.'

# parse config file and start nodes
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
    	id=$( echo $line | awk '{ print $1 }' )
        host=$( echo $line | awk '{ print $2 }' )
        ssh $NETID@$host java -cp $BINARYDIR $PROGRAM $id $CONFIGFILE $OUTPUTDIR &
        n=$(( n+1 ))
    done
)
