#!/bin/sh

set -x

cd /elasticsearch

echo -n "### Checking if RPM exists..."
if [ -e target/rpm/elasticsearch/RPMS/noarch/elasticsearch-*.rpm ] ; then
  echo "yes. No need to build"
else
  echo "no. Building..."
  mvn -DskipTests rpm:rpm
fi

echo "### Installing RPM package"
rpm -i target/rpm/elasticsearch/RPMS/noarch/elasticsearch-*.rpm

echo "### Changing /etc/default/elasticsearch to support dynamic scripts"
echo "ES_JAVA_OPTS=-Des.script.disable_dynamic=false" >> /etc/sysconfig/elasticsearch

echo "### Starting elasticsearch service via systemd"
/bin/systemctl daemon-reload
/bin/systemctl start elasticsearch.service

sleep 20
curl localhost:9200

echo "### Running tests"
LANG=en_US.UTF-8 mvn test -Dtests.class=org.elasticsearch.test.rest.ElasticsearchRestTests -Dtests.cluster=localhost:9300

