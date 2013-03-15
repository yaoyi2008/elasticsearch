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
package org.elasticsearch.index.mapper.geo;

import static org.elasticsearch.index.mapper.MapperBuilders.doubleField;
import static org.elasticsearch.index.mapper.core.TypeParsers.parsePathType;

import org.elasticsearch.ElasticSearchParseException;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.index.mapper.*;
import org.elasticsearch.index.mapper.core.DoubleFieldMapper;

import java.io.IOException;
import java.util.Map;

/**
 *  "myGeoCircle" : { "point" : "same_possible_format_as_geo_point", "radius" : "2km" }
 */
public class GeoCircleFieldMapper implements Mapper {

    public static String CONTENT_TYPE = "geo_circle";
    public static final ContentPath.Type PATH_TYPE = ContentPath.Type.FULL;

    public static class Builder extends Mapper.Builder<Builder, GeoCircleFieldMapper> {

        private ContentPath.Type pathType = PATH_TYPE;
        private GeoPointFieldMapper.Builder geoPointBuilder= new GeoPointFieldMapper.Builder("location");
        private DoubleFieldMapper.Builder radiusBuilder = doubleField("radius");

        public Builder(String name) {
            super(name);
        }

        public Builder pathType(ContentPath.Type pathType) {
            this.pathType = pathType;
            return this;
        }

        /*
        public Builder location(GeoPointFieldMapper.Builder geoPointBuilder) {
            this.geoPointBuilder = geoPointBuilder;
            return this;
        }

        public Builder radius(DoubleFieldMapper.Builder radiusBuilder) {
            this.radiusBuilder = radiusBuilder;
            return this;
        }
        */

        @Override
        public GeoCircleFieldMapper build(BuilderContext context) {
            ContentPath.Type origPathType = context.path().pathType();
            context.path().pathType(pathType);

            context.path().add(name);
            GeoPointFieldMapper geoPointFieldMapper = geoPointBuilder.build(context);
            DoubleFieldMapper radiusFieldMapper = radiusBuilder.build(context);
            context.path().remove();

            context.path().pathType(origPathType);

            return new GeoCircleFieldMapper(name, pathType, geoPointFieldMapper, radiusFieldMapper);
        }
    }

    public static class TypeParser implements Mapper.TypeParser {

        @Override
        public Mapper.Builder parse(String name, Map<String, Object> node, ParserContext parserContext) throws MapperParsingException {
            Builder builder = new Builder(name);

            for (Map.Entry<String, Object> entry : node.entrySet()) {
                String fieldName = entry.getKey();
                Object fieldNode = entry.getValue();

                if (fieldName.equals("path")) {
                    builder.pathType(parsePathType(name, fieldNode.toString()));
                }
            }

            return builder;
        }
    }


    private String name;
    private final ContentPath.Type pathType;
    private final GeoPointFieldMapper geoPointFieldMapper;
    private final DoubleFieldMapper radiusFieldMapper;

    public GeoCircleFieldMapper(String name, ContentPath.Type pathType, GeoPointFieldMapper geoPointFieldMapper,
                                DoubleFieldMapper radiusFieldMapper) {
        this.name = name;
        this.pathType = pathType;
        this.geoPointFieldMapper = geoPointFieldMapper;
        this.radiusFieldMapper = radiusFieldMapper;
    }


    public DoubleFieldMapper getRadiusFieldMapper() {
        return radiusFieldMapper;
    }

    public GeoPointFieldMapper getGeoPointFieldMapper() {
        return geoPointFieldMapper;
    }

    @Override
    public String name() {
            return this.name;
    }

    @Override
    public void parse(ParseContext context) throws IOException {
        String radiusAsText = null;

        XContentParser parser = context.parser();
        XContentParser.Token token;
        while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
            if (token == XContentParser.Token.FIELD_NAME) {
                String fieldName = parser.currentName();

                if ("location".equals(fieldName)) {
                    parser.nextToken();
                    geoPointFieldMapper.parse(context);

                } else if ("radius".equals(fieldName)) {
                    parser.nextToken();
                    radiusAsText = parser.text();

                } else {
                    parser.nextToken();
                    parser.skipChildren();
                }
            }
        }

        // TODO: throw exception if location or radius is null
        if (radiusAsText == null || radiusAsText.length() == 0) {
            throw new ElasticSearchParseException("geo_circle type needs a 'radius' attribute");
        }

        // Put radius to radiusFieldMapper
        double radius = DistanceUnit.parse(radiusAsText, DistanceUnit.KILOMETERS, DistanceUnit.KILOMETERS);
        context.externalValue(radius);
        radiusFieldMapper.parse(context);
    }

    @Override
    public void merge(Mapper mergeWith, MergeContext mergeContext) throws MergeMappingException {
        // TODO
    }

    @Override
    public void traverse(FieldMapperListener fieldMapperListener) {
        geoPointFieldMapper.traverse(fieldMapperListener);
        radiusFieldMapper.traverse(fieldMapperListener);
    }

    @Override
    public void traverse(ObjectMapperListener objectMapperListener) {
    }

    @Override
    public void close() {
        geoPointFieldMapper.close();
        radiusFieldMapper.close();
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject(name);
        builder.field("type", CONTENT_TYPE);
        if (pathType != PATH_TYPE) {
            builder.field("path", pathType.name().toLowerCase());
        }

        builder.startObject("fields");
        geoPointFieldMapper.toXContent(builder, params);
        radiusFieldMapper.toXContent(builder, params);
        builder.endObject();

        builder.endObject();

        return builder;
    }
}
