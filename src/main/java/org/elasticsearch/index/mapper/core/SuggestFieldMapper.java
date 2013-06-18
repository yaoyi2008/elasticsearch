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
package org.elasticsearch.index.mapper.core;

import com.google.common.collect.Lists;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.NumericTokenStream;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttributeImpl;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.util.Attribute;
import org.apache.lucene.util.BytesRef;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.index.analysis.NamedAnalyzer;
import org.elasticsearch.index.analysis.SuggestTokenFilter;
import org.elasticsearch.index.mapper.*;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class SuggestFieldMapper implements Mapper {

    public static final String CONTENT_TYPE = "suggest";

    public static class Defaults extends AbstractFieldMapper.Defaults {
        public static final FieldType FIELD_TYPE = new FieldType(AbstractFieldMapper.Defaults.FIELD_TYPE);

        static {
            FIELD_TYPE.freeze();
        }
    }

    public static class Builder extends Mapper.Builder<Builder, SuggestFieldMapper> {

        private NamedAnalyzer searchAnalyzer;
        private NamedAnalyzer indexAnalyzer;
        private String suggester;
        private ContentPath.Type pathType;

        public Builder(String name) {
            super(name);
            //builder = this;
        }

        public Builder searchAnalyzer(NamedAnalyzer searchAnalyzer) {
            this.searchAnalyzer = searchAnalyzer;
            return this;
        }

        public Builder indexAnalyzer(NamedAnalyzer indexAnalyzer) {
            this.indexAnalyzer = indexAnalyzer;
            return this;
        }

        public Builder suggester(String suggester) {
            this.suggester = suggester;
            return this;
        }

        public Builder pathType(ContentPath.Type pathType) {
            this.pathType = pathType;
            return this;
        }

        @Override
        public SuggestFieldMapper build(Mapper.BuilderContext context) {
            return new SuggestFieldMapper(name, pathType, indexAnalyzer, searchAnalyzer, suggester);
        }
    }

    /*
    "myFooField" {
        "type" : "suggest"
        "index_analyzer" : "stopword",
        "search_analyzer" : "simple",
        "suggester" : "analyzing_prefix"
    }
    */
    public static class TypeParser implements Mapper.TypeParser {

        @Override
        public Mapper.Builder<?, ?> parse(String name, Map<String, Object> node, ParserContext parserContext) throws MapperParsingException {
            SuggestFieldMapper.Builder builder = new SuggestFieldMapper.Builder(name);

            for (Map.Entry<String, Object> entry : node.entrySet()) {
                String fieldName = entry.getKey();
                Object fieldNode = entry.getValue();

                if (fieldName.equals("type")) continue;

                if (fieldName.equals("index_analyzer") || fieldName.equals("indexAnalyzer")) {
                    builder.indexAnalyzer(parserContext.analysisService().analyzer(fieldNode.toString()));
                } else if (fieldName.equals("search_analyzer") || fieldName.equals("searchAnalyzer")) {
                    builder.searchAnalyzer(parserContext.analysisService().analyzer(fieldNode.toString()));
                } else if (fieldName.equals("suggester")) {
                    builder.suggester(fieldNode.toString());
                }
            }

            if (builder.searchAnalyzer == null) {
                builder.searchAnalyzer(parserContext.analysisService().defaultSearchAnalyzer());
            }

            if (builder.indexAnalyzer == null) {
                builder.indexAnalyzer(parserContext.analysisService().defaultIndexAnalyzer());
            }

            return builder;
        }
    }

    private final String name;
    private final NamedAnalyzer indexAnalyzer;
    private final NamedAnalyzer searchAnalyzer;
    private final String suggester;

    public SuggestFieldMapper(String name, ContentPath.Type pathType, NamedAnalyzer indexAnalyzer, NamedAnalyzer searchAnalyzer, String suggester) {
        this.name = name;
        this.indexAnalyzer = indexAnalyzer;
        this.searchAnalyzer = searchAnalyzer;
        this.suggester = suggester;
    }

    @Override
    public String name() {
        return name;
    }

    /*
    "myFooField" : {
        "input" : [ "The Prodigy Firestarter", "Firestarter"],
        "surface_form" : "The Prodigy, Firestarter",
        "weight" : 42,
        "payload" : "whatever"
    }

    "myFooField" {
        "input" : [ "The Prodigy Firestarter", "Firestarter"],
        "surface_form" : "The Prodigy, Firestarter"
    }

    "myFooField" {
        "input" : [ "The Prodigy Firestarter", "Firestarter"]
    }

    "myFooField" : [ "The Prodigy Firestarter", "Firestarter"]
     */
    @Override
    public void parse(ParseContext context) throws IOException {
        XContentParser parser = context.parser();
        XContentParser.Token token = parser.currentToken();

        String surfaceForm = null;
        String payload = null;
        int weight = -1;
        List<String> inputs = Lists.newArrayListWithExpectedSize(4);

        if (token == XContentParser.Token.START_ARRAY) {
            while ((token = parser.nextToken()) != XContentParser.Token.END_ARRAY) {
                inputs.add(parser.text());
            }
        } else {
            String currentFieldName = null;
            while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
                if (token == XContentParser.Token.FIELD_NAME) {
                    currentFieldName = parser.currentName();
                } else if (token == XContentParser.Token.VALUE_STRING) {
                    if ("surface_form".equals(currentFieldName)) {
                        surfaceForm = parser.text();
                    } else if ("payload".equals(currentFieldName)) {
                        payload = parser.text();
                    }
                } else if (token == XContentParser.Token.VALUE_NUMBER) {
                    if ("weight".equals(currentFieldName)) {
                        weight = parser.intValue();
                    }
                } else if (token == XContentParser.Token.START_ARRAY) {
                    if ("input".equals(currentFieldName)) {
                        while ((token = parser.nextToken()) != XContentParser.Token.END_ARRAY) {
                            inputs.add(parser.text());
                        }
                    }
                }
            }
        }

        // TODO: This is clearly wrong
        for (String input : inputs) {

            //Field field = new StringFieldMapper.StringField(name, input, FIELD_TYPE);
            //TokenStream tokenStream = field.tokenStream(indexAnalyzer.analyzer());

            TokenStream tokenStream = indexAnalyzer.tokenStream("", new StringReader(input));
            TokenStream suggestTokenStream = new SuggestTokenFilter(tokenStream, surfaceForm, payload.getBytes(), weight);
            Field suggestField = new TextField(name, suggestTokenStream);
            context.doc().add(suggestField);

            /*
            StringFieldMapper.StringTokenStream stringTokenStream = new StringFieldMapper.StringTokenStream().setValue(input);
            TokenStream tokenStream = new SuggestTokenFilter(stringTokenStream, surfaceForm, payload.getBytes(), weight);
            Analyzer analyzer = context.analysisService().analyzer(indexAnalyzer.name()).analyzer();
            */

            //Field suggestField = new TextField(name, tokenStream);
            //context.doc().add(new Field(name, tokenStream));
        }
        //Field field = new Field(name, inputs.get(0), new FieldType(AbstractFieldMapper.Defaults.FIELD_TYPE));
        //field.tokenStreamValue().addAttributeImpl(new PayloadAttributeImpl(new BytesRef(payload)));
        //field.tokenStreamValue().addAttributeImpl(new NumericTokenStream.NumericTermAttributeImpl());
    }

    public static final FieldType FIELD_TYPE = new FieldType();

    static {
        FIELD_TYPE.setIndexed(true);
        FIELD_TYPE.setTokenized(true);
        FIELD_TYPE.setStored(true);
        FIELD_TYPE.setStoreTermVectors(false);
        FIELD_TYPE.setOmitNorms(false);
        FIELD_TYPE.setIndexOptions(FieldInfo.IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
        FIELD_TYPE.freeze();
    }

    @Override
    public void merge(Mapper mergeWith, MergeContext mergeContext) throws MergeMappingException {
    }

    @Override
    public void traverse(FieldMapperListener fieldMapperListener) {
    }

    @Override
    public void traverse(ObjectMapperListener objectMapperListener) {
    }

    @Override
    public void close() {
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject(name);
        builder.field("type", CONTENT_TYPE);
        builder.field("index_analyzer", indexAnalyzer.name());
        builder.field("search_analyzer", searchAnalyzer.name());
        builder.field("suggester", suggester);
        builder.endObject();
        return builder;
    }
}
