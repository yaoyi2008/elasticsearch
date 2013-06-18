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
package org.elasticsearch.index.codec.postingsformat;

import org.apache.lucene.codecs.*;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.SegmentReadState;
import org.apache.lucene.index.SegmentWriteState;
import org.apache.lucene.index.Terms;

import java.io.IOException;
import java.util.Iterator;

/**
 *
 */
public class SuggestPostingsFormat extends PostingsFormat {

    public static final String CODEC_NAME = "Suggest";
    public static final int BLOOM_CODEC_VERSION = 1;
    public static final String EXTENSION = "sgst";

    private PostingsFormat delegatePostingsFormat;

    public SuggestPostingsFormat(PostingsFormat delegatePostingsFormat) {
        super(CODEC_NAME);
        this.delegatePostingsFormat = delegatePostingsFormat;
    }

    // Used only by core Lucene at read-time via Service Provider instantiation -
    // do not use at Write-time in application code.
    public SuggestPostingsFormat() {
        super(CODEC_NAME);
    }

    @Override
    public SuggestFieldsConsumer fieldsConsumer(SegmentWriteState state) throws IOException {
        if (delegatePostingsFormat == null) {
            throw new UnsupportedOperationException("Error - " + getClass().getName()
                    + " has been constructed without a choice of PostingsFormat");
        }
        return new SuggestFieldsConsumer(
                delegatePostingsFormat.fieldsConsumer(state), state,
                delegatePostingsFormat);
    }

    @Override
    public SuggestFieldsProducer fieldsProducer(SegmentReadState state) throws IOException {
        return new SuggestFieldsProducer(state);
    }

    private class SuggestFieldsConsumer extends FieldsConsumer {

        public SuggestFieldsConsumer(FieldsConsumer fieldsConsumer, SegmentWriteState state, PostingsFormat delegatePostingsFormat) {
        }

        @Override
        public TermsConsumer addField(FieldInfo field) throws IOException {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public void close() throws IOException {
            //To change body of implemented methods use File | Settings | File Templates.
        }
    }

    private class SuggestFieldsProducer extends FieldsProducer {

        public SuggestFieldsProducer(SegmentReadState state) {

        }

        @Override
        public void close() throws IOException {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public Iterator<String> iterator() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public Terms terms(String field) throws IOException {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public int size() {
            return 0;  //To change body of implemented methods use File | Settings | File Templates.
        }
    }
}
