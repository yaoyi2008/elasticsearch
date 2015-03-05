#!/bin/sh

echo "### Installing debian package"
dpkg -i /elasticsearch/target/releases/*.deb

echo "### Changing /etc/default/elasticsearch to support dynamic scripts"
echo "ES_JAVA_OPTS=-Des.script.disable_dynamic=false" >> /etc/default/elasticsearch

echo "### Starting elasticsearch service"
service elasticsearch start

