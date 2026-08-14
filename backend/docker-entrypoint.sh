#!/bin/sh
set -e

if [ -d /data/documents ]; then
  chown -R spring:spring /data/documents 2>/dev/null || true
fi

exec su -s /bin/sh spring -c "exec java -jar app.jar"
