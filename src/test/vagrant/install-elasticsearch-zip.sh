#!/bin/sh

cd /elasticsearch

echo "### Installing zip archive"
cd target/releases
rm -fr zip
unzip *.zip -d zip
cd zip/elasticsearch-*

echo "### Starting elasticsearch in background"
bin/elasticsearch --script.disable_dynamic=false -d

