#!/bin/sh

cd /elasticsearch

echo "### Installing debian package"
dpkg -i target/releases/*.deb

echo "### Changing /etc/default/elasticsearch to support dynamic scripts"
echo "ES_JAVA_OPTS=-Des.script.disable_dynamic=false" >> /etc/default/elasticsearch

echo "### Starting elasticsearch service"
service elasticsearch start

echo "### Running tests"
LANG=en_US.UTF-8 mvn test -Dtests.class=org.elasticsearch.test.rest.ElasticsearchRestTests -Dtests.cluster=localhost:9300

