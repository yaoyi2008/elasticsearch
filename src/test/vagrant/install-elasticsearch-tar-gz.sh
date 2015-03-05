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

