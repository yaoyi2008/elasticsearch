#!/bin/sh

cd /elasticsearch

echo "### Installing tarball"
cd target/releases
rm -fr inst
mkdir inst
tar xzvf *.tar.gz -C inst
cd inst ; cd elasticsearch-*

echo "### Starting elasticsearch in background"
bin/elasticsearch --script.disable_dynamic=false -d

echo "### Running tests"
cd /elasticsearch 
LANG=en_US.UTF-8 mvn test -Dtests.class=org.elasticsearch.test.rest.ElasticsearchRestTests -Dtests.cluster=localhost:9300

