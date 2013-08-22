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
package org.elasticsearch.test.unit.index.mapper.completion;

import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.elasticsearch.Version;
import org.elasticsearch.search.suggest.completion.CompletionSuggestHighlighter;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class CompletionSuggestHighlighterTests {

    private CompletionSuggestHighlighter highlighter;

    @Before
    public void before() {
        highlighter = new CompletionSuggestHighlighter(new SimpleAnalyzer(Version.CURRENT.luceneVersion));
    }

    @Test
    public void testThatBasicHighlightingWorks() throws Exception {
        assertThat(highlighter.highlight("f", "Fo", "<b>", "</b>"), is("<b>F</b>o"));
        assertThat(highlighter.highlight("f", "Foo Fighters", "<b>", "</b>"), is("<b>Foo Fighters</b>"));
        assertThat(highlighter.highlight("foo fighters", "Foo Fighters", "<b>", "</b>"), is("<b>F</b>oo Fighters"));
        assertThat(highlighter.highlight("the best band of the world", "The Best Band Of The World Is Here", "<b>", "</b>"), is("<b>The Best Band Of The World</b> Is Here"));
    }
}

/*
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.junit.Before;
import org.junit.Test;

public class HighlighterTest {

    private Highlighter highlighter;

    @Before
    public void before() {
        highlighter = new Highlighter(new SuggestAnalyzer(false));
    }

    @Test
    public void testQueryIsSubstringOfSurfaceForm()
        throws IOException, InterruptedException, ExecutionException {

        String query = "prodo";
        Record record = new Record("The Prodigy", "prodigy", EntityType.User, 2, 10);
        Pair<Integer, Integer> indices = highlighter.findHighlightIndices(query, record);

        assertNotNull(indices);

        assertEquals(4, (int) indices.getFirst());
        assertEquals(9, (int) indices.getLast());

        assertEquals("The <Prodi>gy", highlighter.highlight(query, record, "<", ">"));
    }

    @Test
    public void testSurfaceFormMatchesKey()
        throws IOException, InterruptedException, ExecutionException {

        String query = "foo fig";
        Record record = new Record("Foo Fighters", "foofighters", EntityType.User, 1, 25);
        Pair<Integer, Integer> indices = highlighter.findHighlightIndices(query, record);

        assertNotNull(indices);

        assertEquals(0, (int) indices.getFirst());
        assertEquals(7, (int) indices.getLast());

        assertEquals("<Foo Fig>hters", highlighter.highlight(query, record, "<", ">"));
    }

}
*/

/*

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;

public final class Highlighter {

    private final Analyzer analyzer;

    public Highlighter(final Analyzer analyzer) {
        this.analyzer = analyzer;
    }

    public Pair<Integer, Integer> findHighlightIndices(final String query, final Record record) {
        final String key = record.key;
        final String surfaceForm = record.surfaceForm;

        char[] surface = new char[surfaceForm.length()];
        Arrays.fill(surface, ' ');
        TokenStream tokenStream = null;

        try {
            tokenStream = analyzer.tokenStream("", new StringReader(surfaceForm));
            tokenStream.reset();

            CharTermAttribute termAttr = tokenStream.addAttribute(CharTermAttribute.class);
            OffsetAttribute offsetAttr = tokenStream.addAttribute(OffsetAttribute.class);
            PositionIncrementAttribute posIncAttr =
                    tokenStream.addAttribute(PositionIncrementAttribute.class);

            int endOffset = 0;
            while (tokenStream.incrementToken()) {
                if (posIncAttr.getPositionIncrement() == 0) {
                    continue;
                }

                endOffset = offsetAttr.endOffset();
                char[] buffer = termAttr.buffer();

                System.arraycopy(buffer, 0, surface, offsetAttr.startOffset(), termAttr.length());
            }

            int keyLen = key.trim().length();
            for (int i = endOffset - 1; i >= 0; i--) {
                switch(surface[i]) {
                    case ' ' :
                    case ',' :
                        break;
                    default:
                        keyLen--;
                }

                if (keyLen == 0) {
                    return new Pair<Integer, Integer>(i, findEndIndex(query, surfaceForm, i));
                }
            }

            // nothing to highlight
            // FIXME: is this an exceptional case?
            return null;

        } catch (final IOException e) {
            // TODO: deal with this
            return null;

        } finally {
            try {
                tokenStream.close();
            } catch (IOException e) {
                // TODO: log it, but otherwise ignorable
            }
        }
    }

    public String highlight(
            final String query, final Record record, final String before, final String after) {

        final Pair<Integer, Integer> index = findHighlightIndices(query, record);

        StringBuilder sb = new StringBuilder();
        sb.append(record.surfaceForm.substring(0, index.getFirst()));
        sb.append(before);
        sb.append(record.surfaceForm.substring(index.getFirst(), index.getLast()));
        sb.append(after);
        sb.append(record.surfaceForm.substring(index.getLast(), record.surfaceForm.length()));

        return sb.toString();
    }

    private static int findEndIndex(
            final String query, final String surfaceForm, final int startIndex) {

        final char[] surfaceFormChars = surfaceForm.toCharArray();
        final int queryLen = length(query);

        int endIndex = startIndex;
        int charsConsumed = 0;

        for (int j = startIndex; j < surfaceFormChars.length;) {

            int codePoint = Character.codePointAt(surfaceFormChars, j);
            int charCount = Character.charCount(codePoint);
            endIndex += charCount;

            if (!Character.isWhitespace(codePoint) && codePoint != ',') {
                charsConsumed += charCount;
                if (charsConsumed >= queryLen) {
                    break;
                }
            }

            j += charCount;
        }

        return endIndex;
    }

    private static int length(final String str) {
        final char[] array = str.toCharArray();

        int length = 0;
        for (int i = 0; i < array.length;) {
            int codePoint = Character.codePointAt(array, i);
            if (!Character.isWhitespace(codePoint)) {
                length++;
            }
            i += Character.charCount(codePoint);
        }

        return length;
    }

}
*/
