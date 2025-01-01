#! /bin/bash
cd ..

set -o allexport
source env/.local.env
set +o allexport

java -jar target/StatefulStreamDedup-1.0-SNAPSHOT.jar -e local -c src/main/resources/application.conf -j KAFKA_STREAM_DEDUP
