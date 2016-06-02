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

package org.elasticsearch.common.io;

import org.elasticsearch.test.ESTestCase;

import java.io.InputStream;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ProgressInputStreamTests extends ESTestCase {

    private InputStream inputStream = mock(InputStream.class);

    public void testThatProgressListenerIsCalled() throws Exception {
        ProgressInputStream.ProgressListener listener = mock(ProgressInputStream.ProgressListener.class);
        int readInvocations = randomIntBetween(1, 250);
        ProgressInputStream is = new ProgressInputStream(inputStream, listener, readInvocations);

        for (int i = 0; i < readInvocations; i++) {
            is.checkProgress(1);
        }
        is.checkProgress(-1);

        // one more invocation for the final -1 read
        verify(listener, times(Math.min(100, readInvocations+1))).onProgress(anyInt());
    }

    public void testThatProgressListenerIsCalledOnUnexpectedCompletion() throws Exception {
        ProgressInputStream.ProgressListener listener = mock(ProgressInputStream.ProgressListener.class);
        ProgressInputStream is = new ProgressInputStream(inputStream, listener, 2);
        is.checkProgress(-1);
        verify(listener, times(1)).onProgress(anyInt());
        verify(listener, times(1)).onProgress(100);
    }

    public void testThatProgressListenerReturnsMaxValueOnWrongExpectedSize() throws Exception {
        ProgressInputStream.ProgressListener listener = mock(ProgressInputStream.ProgressListener.class);
        ProgressInputStream is = new ProgressInputStream(inputStream, listener, 2);

        is.checkProgress(1);
        verify(listener, times(1)).onProgress(50);

        is.checkProgress(3);
        verify(listener, times(1)).onProgress(99);

        is.checkProgress(-1);
        verify(listener, times(1)).onProgress(100);
    }
}