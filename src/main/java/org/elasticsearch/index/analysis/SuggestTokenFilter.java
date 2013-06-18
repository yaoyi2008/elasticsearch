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
package org.elasticsearch.index.analysis;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.TokenStreamToAutomaton;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.util.AttributeSource;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.IntsRef;
import org.apache.lucene.util.automaton.Automaton;
import org.apache.lucene.util.automaton.SpecialOperations;
import org.apache.lucene.util.fst.Util;

import java.io.IOException;
import java.io.Reader;

/**
 *
 */
public final class SuggestTokenFilter extends TokenStream {

    private final CharTermAttribute termAttr;
    private final PayloadAttribute payloadAttr;
    private final PositionIncrementAttribute posAttr;
    //private final SuggestAttribute suggestAttr = addAttribute(SuggestAttribute.class);

    private final TokenStream input;
    private String surfaceForm;
    private byte[] payload;
    private IntsRef[] finiteStrings;
    private int finiteStringPos = 0;
    private int weight;

    public SuggestTokenFilter(TokenStream input, String surfaceForm, byte[] payload, int weight) {
        //super(new AttributeSource());
        super(input);
        termAttr = addAttribute(CharTermAttribute.class);
        payloadAttr = addAttribute(PayloadAttribute.class);
        posAttr = addAttribute(PositionIncrementAttribute.class);
        this.input = input;
        this.payload = payload;
        this.surfaceForm = surfaceForm;
        this.weight = weight;
    }

    @Override
    public boolean incrementToken() throws IOException {
        if (finiteStrings == null) {
            // Analyze surface form:
            // TokenStream ts = indexAnalyzer.tokenStream("", new StringReader(surfaceForm.utf8ToString()));
            // Create corresponding automaton: labels are bytes
            // from each analyzed token, with byte 0 used as
            // separator between tokens:
            Automaton automaton = new TokenStreamToAutomaton().toAutomaton(input);
            // replaceSep(automaton); // TODO
            assert SpecialOperations.isFinite(automaton);

            // Get all paths from the automaton (there can be
            // more than one path, eg if the analyzer created a
            // graph using SynFilter or WDF):
            int maxGraphExpansions = 1;

            // TODO: we could walk & add simultaneously, so we
            // don't have to alloc [possibly biggish]
            // intermediate HashSet in RAM:
            finiteStrings = SpecialOperations.getFiniteStrings(automaton, maxGraphExpansions).toArray(new IntsRef[0]);
        }

        BytesRef scratch = new BytesRef();
        if (finiteStringPos < finiteStrings.length) {
            posAttr.setPositionIncrement(1); // always inc by one - no payload can be on the same pos
            BytesRef spare = new BytesRef();
            Util.toBytesRef(finiteStrings[finiteStringPos++], spare); // now we have UTF-8
            // length of the analyzed text (FST input)
            if (scratch.length > Short.MAX_VALUE-2) {
                throw new IllegalArgumentException("cannot handle analyzed forms > " + (Short.MAX_VALUE-2) + " in length (got " + scratch.length + ")");
            }
            // The termAttribute contains the whole input tokenstream automaton
            termAttr.setEmpty();
            termAttr.append(spare.utf8ToString());
            payloadAttr.setPayload(new BytesRef(surfaceForm + "|" + new String(payload) + "|" + weight)); //"surface|payload|weight";

            //suggestAttr.setAutomaton(spare);
            //suggestAttr.setSurfaceForm(new BytesRef(surfaceForm));
            //suggestAttr.setPayload(new BytesRef(payload));
            //suggestAttr.setWeight(weight);
            //ESLoggerFactory.getLogger(getClass().getName()).error("GOT SUGGEST ATTR {}", suggestAttr);

            return true;
        }

        return false;
    }

    /*
    public static interface SuggestAttribute extends Attribute, TermToBytesRefAttribute {
        String[] suggest(String query);

        void setSurfaceForm(BytesRef surfaceForm);

        void setPayload(BytesRef payload);

        void setWeight(int weight);

        void setAutomaton(BytesRef automaton);
    }

    public static class SuggestAttributeImpl extends AttributeImpl implements SuggestAttribute {

        private static int MIN_BUFFER_SIZE = 10;
        private BytesRef payload;
        private BytesRef surfaceForm;
        private int weight;
        private BytesRef automaton;

        @Override
        public void clear() {
            //throw new ElasticSearchException("not yet implemented");
        }

        @Override
        public void copyTo(AttributeImpl target) {
            throw new ElasticSearchException("not yet implemented");
        }

        @Override
        public String[] suggest(String query) {
            // TODO: check the automaton
            // TODO: execute some query?!
            // TODO: include weight in automaton query?!
            // TODO: include payload

            if (surfaceForm.isValid()) {
                return new String[] { surfaceForm.utf8ToString() };
            }
            return new String[]{};
        }

        @Override
        public void setSurfaceForm(BytesRef surfaceForm) {
            this.surfaceForm = surfaceForm;
        }

        @Override
        public void setPayload(BytesRef payload) {
            this.payload = payload;
        }

        @Override
        public void setWeight(int weight) {
            this.weight = weight;
        }

        @Override
        public void setAutomaton(BytesRef automaton) {
            this.automaton = automaton;
        }

        private BytesRef bytes = new BytesRef(MIN_BUFFER_SIZE);

        // not until java 6 @Override
        @Override
        public int fillBytesRef() {
            return 0;//UnicodeUtil.UTF16toUTF8WithHash(termBuffer, 0, termLength, bytes);
        }

        // not until java 6 @Override
        @Override
        public BytesRef getBytesRef() {
            return bytes;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (surfaceForm != null) sb.append(surfaceForm.utf8ToString());
            if (payload != null) sb.append("/payload " + payload.utf8ToString());
            sb.append("/weight " + weight );
            return sb.toString();
        }
    }
    */

    /*
    static {
        try {
            AtomicReader reader = null;
            Terms terms = reader.fields().terms("my_suggest_field");
            TermsEnum iterator = terms.iterator(null);
            SuggestAttribute suggest = iterator.attributes().addAttribute(SuggestAttribute.class);
            suggest.suggest("foo");
        } catch (Exception e) {
        }
    }
    */
}