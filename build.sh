#!/bin/sh
NAME=RoomChat
CP=.
FLAGS=-Xlint
#
CP=$CP:~/hey0/bin/Minecraft_Mod.jar
rm *.class
rm ~/hey0/bin/plugins/$NAME.jar
echo Building
find -L -name \*.java |xargs javac -cp $CP $FLAGS || exit $?
echo Creating manifest
echo >manifest <<EOF
Manifest-Version: 1.0
Created-By: 1.6.0 (Sun Microsystems Inc.)
Main-Class: $NAME
Class-Path: $CP
EOF
echo Linking
find -name \*.class |xargs jar cmf manifest ~/hey0/bin/plugins/$NAME.jar || exit $?
