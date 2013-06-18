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

import com.google.common.collect.Lists;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.suggest.SuggestResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.search.suggest.nrt.NrtSuggestionBuilder;
import org.elasticsearch.test.integration.AbstractNodesTests;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.List;

import static org.elasticsearch.cluster.metadata.IndexMetaData.SETTING_NUMBER_OF_REPLICAS;
import static org.elasticsearch.cluster.metadata.IndexMetaData.SETTING_NUMBER_OF_SHARDS;
import static org.elasticsearch.common.settings.ImmutableSettings.settingsBuilder;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

/**
 *
 */
public class NrtSuggestSearchTests extends AbstractNodesTests {

    private static String INDEX = "test";
    public static String TYPE = "testType";
    public static String FIELD = "testField";
    private Client client;

    @BeforeClass
    public void createNodes() throws Exception {
        startNode("server1");
        // be quicker for now
        //startNode("server2");
        client = client("server1");
    }

    @AfterClass
    public void closeNodes() {
        client.close();
        closeAllNodes();
    }


    @Test
    public void testThatNrtSuggestionsWork() throws Exception {
        createIndexAndMapping();
        createData();

        GetResponse getResponse = client.prepareGet(INDEX, TYPE, "1").execute().actionGet();

        SuggestResponse suggestResponse = client.prepareSuggest(INDEX).addSuggestion(
                new NrtSuggestionBuilder("foo").field(FIELD).text("f").size(10)
        ).execute().actionGet();

        assertThat(suggestResponse.getSuggest().size(), is(1));
        Suggest.Suggestion<Suggest.Suggestion.Entry<Suggest.Suggestion.Entry.Option>> suggestion = suggestResponse.getSuggest().getSuggestion("foo");

        assertThat(suggestion.getEntries().size(), is(1));
        List<String> names = getNames(suggestion.getEntries().get(0));
        assertThat(names, hasSize(1));
        assertThat(names.get(0), is("Foo Fighters"));
    }

    private List<String> getNames(Suggest.Suggestion.Entry<Suggest.Suggestion.Entry.Option> suggestEntry) {
        List<String> names = Lists.newArrayList();
        for (Suggest.Suggestion.Entry.Option entry : suggestEntry.getOptions()) {
            names.add(entry.getText().string());
        }

        return names;
    }

    private void createIndexAndMapping() throws IOException {
        client.admin().indices().prepareDelete().execute().actionGet();
        client.admin().indices().prepareCreate(INDEX)
                .setSettings(settingsBuilder()
                        .put(SETTING_NUMBER_OF_SHARDS, 1)
                        .put(SETTING_NUMBER_OF_REPLICAS, 0))
                .execute().actionGet();
        client.admin().indices().preparePutMapping(INDEX).setType(TYPE).setSource(jsonBuilder().startObject()
                .startObject(TYPE).startObject("properties")
                .startObject(FIELD)
                .field("type", "suggest")
                .field("index_analyzer", "standard")
                .field("search_analyzer", "simple")
                .field("suggester", "analyzing_prefix")
                .endObject()
                .endObject().endObject()
                .endObject()).execute().actionGet();
        client.admin().cluster().prepareHealth(INDEX).setWaitForGreenStatus().execute().actionGet();
    }

    private void createData() throws IOException {
        client.prepareIndex(INDEX, TYPE, "1")
                .setSource(jsonBuilder()
                        .startObject().startObject(FIELD)
                        .startArray("input").value("foo").endArray()
                        .field("surface_form", "Foo Fighters")
                        .field("payload", "whatever")
                        .field("weight", 20)
                        .endObject()
                        .endObject()
                )
                .setRefresh(true)
                .execute().actionGet();
    }
}
