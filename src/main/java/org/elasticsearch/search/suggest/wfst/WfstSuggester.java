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
package org.elasticsearch.search.suggest.wfst;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.spell.TermFreqIterator;
import org.apache.lucene.search.suggest.Lookup;
import org.apache.lucene.util.CharsRef;
import org.elasticsearch.common.text.BytesText;
import org.elasticsearch.common.text.StringText;
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.search.suggest.SuggestContextParser;
import org.elasticsearch.search.suggest.Suggester;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * DONE: copy wfst completion lookup and make it cheap :)
 *
 */
public class WfstSuggester implements Suggester<WfstSuggestionContext> {

    // static per JVM... erm, uhm, sucks, can be changed as this is guice wired
    // make me a frigging google cache in order to get it nice
    private static final Map<IndexReader, IndexReaderLookup> indexReaderLookups = Maps.newHashMap();

    private static final IndexReader.ReaderClosedListener readerClosedListener = new IndexReader.ReaderClosedListener() {
        @Override
        public void onClose(IndexReader reader) {
            indexReaderLookups.get(reader).clear();
            indexReaderLookups.remove(reader);
        }
    };

    @Override
    public Suggest.Suggestion<? extends Suggest.Suggestion.Entry<? extends Suggest.Suggestion.Entry.Option>> execute(String name, WfstSuggestionContext suggestion, IndexReader indexReader, CharsRef spare) throws IOException {
        WfstSuggestion wfstSuggestion = new WfstSuggestion(name, suggestion.getSize());
        WfstSuggestion.Entry wfstSuggestionEntry = new WfstSuggestion.Entry(new StringText(suggestion.getText().utf8ToString()), 0, suggestion.getText().toString().length());
        wfstSuggestion.addTerm(wfstSuggestionEntry);

        String fieldName = suggestion.getField();

        loadLookupsForAtomicReaders(fieldName, indexReader.leaves());

        // do the suggestion dance per segment
        for (AtomicReaderContext atomicReaderContext : indexReader.leaves()) {
            AtomicReader atomicReader = atomicReaderContext.reader();

            List<Lookup.LookupResult> lookupResults = indexReaderLookups.get(atomicReader).lookup(fieldName, suggestion.getText().utf8ToString(), suggestion.getSize());
            for (Lookup.LookupResult lookupResult : lookupResults) {
                wfstSuggestionEntry.addOption(new WfstSuggestion.Entry.Option(new StringText(lookupResult.key.toString()), 1.0f));
            }
        }

        return wfstSuggestion;
    }

    @Override
    public String[] names() {
        return new String[] { "wfst" };
    }

    @Override
    public SuggestContextParser getContextParser() {
        return new WfstSuggestParser(this);
    }


    public void loadLookupsForAtomicReaders(String field, List<AtomicReaderContext> atomicReaderContexts) throws IOException {
        for (AtomicReaderContext atomicReaderContext : atomicReaderContexts) {
            AtomicReader atomicReader = atomicReaderContext.reader();

            if (!indexReaderLookups.containsKey(atomicReader)) {
                indexReaderLookups.put(atomicReader, new IndexReaderLookup());
                atomicReader.addReaderClosedListener(readerClosedListener);
            }

            if (!indexReaderLookups.get(atomicReader).contains(field)) {
                WFSTCompletionLookup wfstCompletionLookup = new WFSTCompletionLookup();
                TermsEnum termsEnum = atomicReader.terms(field).iterator(null);
                wfstCompletionLookup.build(new TermFreqIterator.TermFreqIteratorWrapper(termsEnum));
                indexReaderLookups.get(atomicReader).get(field).add(wfstCompletionLookup);
            }
        }
    }

    private static final class IndexReaderLookup {
        // yeah, hashmaps, not efficient, need to fix
        private final Map<String, List<WFSTCompletionLookup>> lookups = Maps.newHashMap();

        public boolean contains(String field) {
            return lookups.containsKey(field);
        }

        public List<WFSTCompletionLookup> get(String field) {
            if (!lookups.containsKey(field)) {
                lookups.put(field, Lists.<WFSTCompletionLookup>newArrayList());
            }

            return lookups.get(field);
        }

        public List<Lookup.LookupResult> lookup(String field, String term, int size) {
            List<Lookup.LookupResult> lookupResults = Lists.newArrayList();

            for (WFSTCompletionLookup fieldLookup : get(field)) {
                lookupResults.addAll(fieldLookup.lookup(term, false, size));
            }

            return lookupResults;
        }

        public void clear() {
            lookups.clear();
        }
    }

}
