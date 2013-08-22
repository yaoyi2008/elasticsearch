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
package org.elasticsearch.search.suggest.completion;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;

public class CompletionSuggestHighlighter {

    private final Analyzer analyzer;

    public CompletionSuggestHighlighter(Analyzer analyzer) {
        this.analyzer = analyzer;
    }

    public Pair<Integer, Integer> findHighlightIndices(final String query, String surfaceForm) {
        char[] surface = new char[surfaceForm.length()];
        Arrays.fill(surface, ' ');
        TokenStream tokenStream = null;

        try {
            tokenStream = analyzer.tokenStream("", new StringReader(surfaceForm));
            tokenStream.reset();

            CharTermAttribute termAttr = tokenStream.addAttribute(CharTermAttribute.class);
            OffsetAttribute offsetAttr = tokenStream.addAttribute(OffsetAttribute.class);
            PositionIncrementAttribute posIncAttr = tokenStream.addAttribute(PositionIncrementAttribute.class);

            int endOffset = 0;
            while (tokenStream.incrementToken()) {
                if (posIncAttr.getPositionIncrement() == 0) {
                    continue;
                }

                endOffset = offsetAttr.endOffset();
                char[] buffer = termAttr.buffer();

                System.arraycopy(buffer, 0, surface, offsetAttr.startOffset(), termAttr.length());
            }

            //System.out.println(String.format("From surfaceForm %s to %s", surfaceForm,  new String(surface)));
            // TODO: This is clearly wrong, and was extracted from the unit tests
            int keyLen = new String(surface).replaceAll(" ", "").trim().toLowerCase().length();
            for (int i = endOffset - 1; i >= 0; i--) {
                if (surface[i] != ' ') {
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

    public String highlight(final String query, final String surfaceForm, final String before, final String after) {

        //System.out.println(String.format("query %s, length %s, surfaceForm %s", query, length, surfaceForm));
        final Pair<Integer, Integer> index = findHighlightIndices(query, surfaceForm);
        //System.out.println(String.format("start %s end %s", index.getFirst(), index.getLast()));

        StringBuilder sb = new StringBuilder();
        sb.append(surfaceForm.substring(0, index.getFirst()));
        sb.append(before);
        sb.append(surfaceForm.substring(index.getFirst(), index.getLast()));
        sb.append(after);
        sb.append(surfaceForm.substring(index.getLast(), surfaceForm.length()));

        //System.out.println("OUT " + sb);
        return sb.toString();
    }

    private static int findEndIndex(final String query, final String surfaceForm, final int startIndex) {

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

    private class Pair<A, B> {

        private A first;
        private B last;

        public Pair(A first, B last) {
            this.first = first;
            this.last = last;
        }

        public A getFirst() {
            return first;
        }

        private B getLast() {
            return last;
        }
    }
}
