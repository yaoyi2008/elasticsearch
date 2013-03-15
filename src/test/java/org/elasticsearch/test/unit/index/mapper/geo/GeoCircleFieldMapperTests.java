/*
 * Licensed to ElasticSearch and Shay Banon under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. ElasticSearch licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.elasticsearch.test.unit.index.mapper.geo;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.google.common.util.concurrent.AtomicDouble;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.mapper.DocumentMapper;
import org.elasticsearch.index.mapper.DocumentMapperParser;
import org.elasticsearch.index.mapper.MapperParsingException;
import org.elasticsearch.index.mapper.core.DoubleFieldMapper;
import org.elasticsearch.index.mapper.core.StringFieldMapper;
import org.elasticsearch.index.mapper.geo.GeoCircleFieldMapper;
import org.elasticsearch.test.unit.index.mapper.MapperTests;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;

public class GeoCircleFieldMapperTests {

    private DocumentMapper docMapper;

    @BeforeClass
    public void createMapping() throws Exception {
        DocumentMapperParser mapperParser = MapperTests.newParser();
        mapperParser.putTypeParser(GeoCircleFieldMapper.CONTENT_TYPE, new GeoCircleFieldMapper.TypeParser());

        String mapping = jsonBuilder().startObject().startObject("circle")
            .startObject("properties").startObject("circle")
            .field("type", "geo_circle")
            .endObject().endObject()
            .endObject().endObject().string();

        docMapper = mapperParser.parse(mapping);
    }

    @Test
    public void testDefaultConfiguration() throws Exception {
        assertThat(docMapper.mappers().fullName("circle.radius").mapper(), is(instanceOf(DoubleFieldMapper.class)));
        assertThat(docMapper.mappers().fullName("circle.location").mapper(), is(instanceOf(StringFieldMapper.class)));
    }

    @Test
    public void testSimpleMapping() throws Exception {
        XContentBuilder contentBuilder = jsonBuilder().startObject()
                .field("_id", "1")
                .startObject("circle")
                    .startObject("location").field("lat", 0.0).field("lon", 100.0).endObject()
                    .field("radius", "1mi")
                .endObject()
                .endObject();

        Document doc = docMapper.parse(contentBuilder.bytes()).rootDoc();

        DoubleFieldMapper.CustomDoubleNumericField radiusField = (DoubleFieldMapper.CustomDoubleNumericField)
                doc.getField(docMapper.mappers().fullName("circle.radius").mapper().names().indexName());
        assertThat(radiusField, is(notNullValue()));
        assertThat(radiusField.numericAsString(), is("1.609344"));

        IndexableField locationField = doc.getField(docMapper.mappers().fullName("circle.location").mapper().names().indexName());
        assertThat(locationField, is(notNullValue()));
        assertThat(locationField.stringValue(), is("0.0,100.0"));
    }

    @Test(expectedExceptions = MapperParsingException.class)
    public void missingRadiusFieldShouldThrowException() throws IOException {
        XContentBuilder contentBuilder = jsonBuilder().startObject()
                .field("_id", "1")
                .startObject("circle")
                .array("location", 100.0, 0.0)
                .endObject()
                .endObject();

        docMapper.parse(contentBuilder.bytes()).rootDoc();
    }

}
