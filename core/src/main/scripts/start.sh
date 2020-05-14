#!/bin/sh
#
# start a standalone Jetty server 
#

dir=`dirname $0`
if [ "$dir" != "." ]
then
  INST=`dirname $dir`
else
  pwd | grep -e 'bin$' > /dev/null
  if [ $? = 0 ]
  then
    # we are in the bin directory
    INST=".."
  else
    # we are NOT in the bin directory
    INST=`dirname $dir`
  fi
fi

INST=${INST:-.}

#
#Alternatively specify the installation dir here
#
#INST=

cd $INST

#
#Java command 
#
JAVA=java

#
#Options to the Java VM
#
OPTS=""

#
#Memory for the VM
#
MEM=-Xmx128m

#some more defines
DEFS=""

#
#put all jars in lib/ on the classpath
#
JARS=lib/*.jar
CP=.
for JAR in $JARS ; do 
    CP=$CP:$JAR
done

echo Reading code from $CP

#
#go
#
$JAVA ${MEM} ${OPTS} ${DEFS} -cp ${CP} de.fzj.unicore.wsrflite.Kernel $*

