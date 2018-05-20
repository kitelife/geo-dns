#!/usr/bin/env bash

nohup ${JAVA_HOME}/bin/java -Xms128M -Xmx1024M -Dfile.encoding=UTF-8 \
    -cp geo-dns-1.0-SNAPSHOT-jar-with-dependencies.jar GeoNameService > gns.log 2>&1 &