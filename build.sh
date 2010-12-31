#!/bin/sh
NAME=RoomChat
# ':' terminate this!
CP=
FLAGS=-Xlint
# do not edit after this.
CP=Minecraft_Mod.jar:${CP}.
rm *.class
rm $NAME.jar
echo Building
find -L -name \*.java |xargs javac -cp $CP $FLAGS || exit $?
echo Creating manifest
cat >manifest <<EOF
Manifest-Version: 1.0
Created-By: 1.6.0 (Sun Microsystems Inc.)
Main-Class: $NAME
Class-Path: $CP
EOF
echo Linking
find -name \*.class |xargs jar cmf manifest $NAME.jar || exit $?
