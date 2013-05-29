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
package org.elasticsearch.test.integration.search.suggest;

import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.index.mapper.MapperService;
import org.elasticsearch.search.suggest.SuggestContextParser;
import org.elasticsearch.search.suggest.SuggestionSearchContext;

import java.io.IOException;
import java.util.Map;

/**
 *
 */
public class CustomSuggesterContextParser implements SuggestContextParser {

    private CustomSuggester customSuggester;

    @Inject
    public CustomSuggesterContextParser(CustomSuggester customSuggester) {
        this.customSuggester = customSuggester;
    }

    @Override
    public SuggestionSearchContext.SuggestionContext parse(XContentParser parser, MapperService mapperService) throws IOException {
        Map<String, Object> options = parser.map();
        CustomSuggester.CustomSuggestionsContext suggestionContext = new CustomSuggester.CustomSuggestionsContext(customSuggester, options);
        suggestionContext.setField((String) options.get("field"));
        return suggestionContext;
    }

    @Override
    public String[] names() {
        return new String[] {"custom"};
    }
}
