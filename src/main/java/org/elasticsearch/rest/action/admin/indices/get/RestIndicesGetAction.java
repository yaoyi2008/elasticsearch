/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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
package org.elasticsearch.rest.action.admin.indices.get;

import com.carrotsearch.hppc.cursors.ObjectCursor;
import com.carrotsearch.hppc.cursors.ObjectObjectCursor;
import com.google.common.collect.ImmutableList;
import org.elasticsearch.action.admin.cluster.state.ClusterStateRequest;
import org.elasticsearch.action.admin.cluster.state.ClusterStateResponse;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.metadata.AliasMetaData;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentBuilderString;
import org.elasticsearch.index.mapper.ParsedDocument;
import org.elasticsearch.rest.*;
import org.elasticsearch.rest.action.support.RestResponseListener;
import org.elasticsearch.search.warmer.IndexWarmersMetaData;

import java.io.IOException;
import java.util.*;

import static org.elasticsearch.rest.RestRequest.Method.GET;
import static org.elasticsearch.rest.RestStatus.OK;

/**
 *
 */
public class RestIndicesGetAction extends BaseRestHandler {

    @Inject
    public RestIndicesGetAction(Settings settings, Client client, RestController controller) {
        super(settings, client);
        controller.registerHandler(GET, "/{index}", this);
        controller.registerHandler(GET, "/{index}/{type}", this);
        // FIXME
        // need hard to overwrite this one, otherwise that URL is not found
        // I think there are get handlers overwriting each other, need to investigate
        //controller.registerHandler(GET, "/{index}/_mappings", this);
    }

    @Override
    public void handleRequest(final RestRequest request, final RestChannel channel, final Client client) {
        final String[] indexes = Strings.splitStringByCommaToArray(request.param("index"));
        final Set<String> features = request.hasParam("type") ? Strings.splitStringByCommaToSet(request.param("type")) : new HashSet<String>(); // TODO MAKE ME Collections.EMPTY_SET after fixing the rest
        //// FIXME (as in remove)
        // Collides with RestGetMappingAction
        //if (request.uri().contains("/_mappings")) {
        //    features.add("_mappings");
        //}
        final boolean isFeatureSelected = features.size() > 0;

        final ClusterStateRequest clusterStateRequest = new ClusterStateRequest();
        clusterStateRequest.indices(indexes);
        clusterStateRequest.local(request.paramAsBoolean("local", clusterStateRequest.local()));
        clusterStateRequest.listenerThreaded(false);

        client.admin().cluster().state(clusterStateRequest, new RestResponseListener<ClusterStateResponse>(channel) {

            @Override
            public RestResponse buildResponse(ClusterStateResponse response) throws Exception {
                XContentBuilder builder = channel.newBuilder();
                builder.startObject();

                ImmutableOpenMap<String, IndexMetaData> indexMetaDataMap = response.getState().metaData().indices();
                String[] concreteIndices = response.getState().metaData().concreteIndices(IndicesOptions.fromOptions(true, true, true, false), indexes);
                ImmutableOpenMap<String, ImmutableList<IndexWarmersMetaData.Entry>> warmers = response.getState().metaData().findWarmers(concreteIndices, Strings.EMPTY_ARRAY, Strings.EMPTY_ARRAY);

                for (ObjectObjectCursor<String, IndexMetaData> cursor : indexMetaDataMap) {
                    String indexName = cursor.key;
                    IndexMetaData indexMetaData = cursor.value;

                    builder.startObject(indexName);

                    writeSettings(builder, indexMetaData.settings());
                    writeMappings(builder, indexMetaData.mappings());
                    writeAliases(builder, indexMetaData.aliases(), request);
                    writeWarmer(builder, warmers.get(indexName), request);

                    builder.endObject();
                }

                builder.endObject();
                return new BytesRestResponse(OK, builder);
            }

            private void writeWarmer(XContentBuilder builder, ImmutableList<IndexWarmersMetaData.Entry> entries, RestRequest request) throws IOException {
                if (isFeatureSelected && !features.contains("_warmers")) {
                    return;
                }
                if (entries != null && entries.size() > 0) {
                    builder.startObject(Fields.WARMERS);
                    for (IndexWarmersMetaData.Entry warmerEntry : entries) {
                        IndexWarmersMetaData.FACTORY.toXContent(warmerEntry, builder, request);
                    }
                    builder.endObject();
                }
            }

            private void writeSettings(XContentBuilder builder, Settings settings) throws IOException {
                if (isFeatureSelected && !features.contains("_settings")) {
                    return;
                }
                builder.startObject(Fields.SETTINGS);
                for (Map.Entry<String, String> entry : settings.getAsMap().entrySet()) {
                    builder.field(entry.getKey(), entry.getValue());
                }
                builder.endObject();
            }

            private void writeMappings(XContentBuilder builder, ImmutableOpenMap<String, MappingMetaData> mappings) throws IOException {
                if (isFeatureSelected && !features.contains("_mappings")) {
                    return;
                }
                if (mappings.size() > 0) {
                    builder.startObject(Fields.MAPPINGS);

                    for (ObjectObjectCursor<String, MappingMetaData> mappingEntry : mappings) {
                        builder.field(mappingEntry.key);
                        builder.map(mappingEntry.value.sourceAsMap());
                    }

                    builder.endObject();
                }
            }

            private void writeAliases(XContentBuilder builder, ImmutableOpenMap<String, AliasMetaData> aliases, RestRequest request) throws IOException {
                if (isFeatureSelected && !features.contains("_aliases")) {
                    return;
                }
                if (aliases.size() > 0) {
                    builder.startObject(Fields.ALIASES);
                    for (ObjectCursor<AliasMetaData> alias : aliases.values()) {
                        AliasMetaData.Builder.toXContent(alias.value, builder, request);
                    }
                    builder.endObject();
                }
            }

        });
    }

    static class Fields {
        static final XContentBuilderString ALIASES = new XContentBuilderString("aliases");
        static final XContentBuilderString MAPPINGS = new XContentBuilderString("mappings");
        static final XContentBuilderString SETTINGS = new XContentBuilderString("settings");
        static final XContentBuilderString WARMERS = new XContentBuilderString("warmers");
    }

}
