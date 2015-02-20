#!/bin/sh

cd /elasticsearch

echo "### Installing zip archive"
cd target/releases
rm -fr zip
unzip *.zip -d zip
cd zip/elasticsearch-*

echo "### Starting elasticsearch in background"
bin/elasticsearch --script.disable_dynamic=false -d

echo "### Running tests"
cd /elasticsearch 
LANG=en_US.UTF-8 mvn test -Dtests.class=org.elasticsearch.test.rest.ElasticsearchRestTests -Dtests.cluster=localhost:9300

