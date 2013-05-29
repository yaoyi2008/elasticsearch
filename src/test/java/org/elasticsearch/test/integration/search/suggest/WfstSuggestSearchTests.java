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
import org.elasticsearch.action.suggest.SuggestResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.search.suggest.wfst.WfstSuggestion;
import org.elasticsearch.search.suggest.wfst.WfstSuggestionBuilder;
import org.elasticsearch.test.integration.AbstractNodesTests;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.elasticsearch.cluster.metadata.IndexMetaData.SETTING_NUMBER_OF_REPLICAS;
import static org.elasticsearch.cluster.metadata.IndexMetaData.SETTING_NUMBER_OF_SHARDS;
import static org.elasticsearch.common.settings.ImmutableSettings.settingsBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 *
 */
public class WfstSuggestSearchTests extends AbstractNodesTests {

    // TODO: check out what happens after a merge

    private Client client;

    @BeforeClass
    public void createNodes() throws Exception {
        startNode("server1");
        startNode("server2");
        client = client("server1");
    }

    @AfterClass
    public void closeNodes() {
        client.close();
        closeAllNodes();
    }


    @Test
    public void testThatWfstSuggestionsWork() throws Exception {
        createIndexAndData();

        SuggestResponse suggestResponse = client.prepareSuggest("test").addSuggestion(
                new WfstSuggestionBuilder("foo").field("text").text("abc").size(10)
        ).execute().actionGet();

        assertThat(suggestResponse.getSuggest().size(), is(1));
        Suggest.Suggestion<Suggest.Suggestion.Entry<Suggest.Suggestion.Entry.Option>> suggestion = suggestResponse.getSuggest().getSuggestion("foo");
        assertThat(suggestion, instanceOf(WfstSuggestion.class));

        assertThat(suggestion.getEntries().size(), is(1));
        List<String> names = getNames(suggestion.getEntries().get(0));
        assertThat(names, hasSize(10));
        assertThat(names.get(0), is("abc0"));
        assertThat(names.get(1), is("abc1"));
        assertThat(names.get(2), is("abc10"));

        createData("abcd");
        SuggestResponse suggestResponse2 = client.prepareSuggest("test").addSuggestion(
                new WfstSuggestionBuilder("bar").field("text").text("abcd").size(10)
        ).execute().actionGet();
        Suggest.Suggestion<Suggest.Suggestion.Entry<Suggest.Suggestion.Entry.Option>> suggestion2 = suggestResponse2.getSuggest().getSuggestion("bar");
        assertThat(suggestion2.getEntries().size(), is(1));
        List<String> names2 = getNames(suggestion2.getEntries().get(0));
        assertThat(names2, hasSize(10));
    }

    private List<String> getNames(Suggest.Suggestion.Entry<Suggest.Suggestion.Entry.Option> suggestEntry) {
        List<String> names = Lists.newArrayList();
        for (Suggest.Suggestion.Entry.Option entry : suggestEntry.getOptions()) {
            names.add(entry.getText().string());
        }

        return names;
    }

    private void createIndexAndData() throws IOException {
        client.admin().indices().prepareDelete().execute().actionGet();
        client.admin().indices().prepareCreate("test")
                .setSettings(settingsBuilder()
                        .put(SETTING_NUMBER_OF_SHARDS, 1)
                        .put(SETTING_NUMBER_OF_REPLICAS, 0))
                .execute().actionGet();
        client.admin().cluster().prepareHealth("test").setWaitForGreenStatus().execute().actionGet();

        createData("abc");
    }

    private void createData(String data) throws IOException {
        for (int i = 0; i < 15; i++) {
            client.prepareIndex("test", "type1")
                    .setSource(XContentFactory.jsonBuilder()
                            .startObject()
                            .field("text", data + i)
                            .endObject()
                    )
                    .execute().actionGet();
        }

        client.admin().indices().prepareRefresh().execute().actionGet();
    }
}
