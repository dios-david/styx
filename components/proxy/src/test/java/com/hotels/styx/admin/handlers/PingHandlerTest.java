/**
 * Copyright (C) 2013-2017 Expedia Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hotels.styx.admin.handlers;

import com.hotels.styx.api.messages.FullHttpResponse;
import org.testng.annotations.Test;

import static com.hotels.styx.api.HttpRequest.Builder.get;
import static com.hotels.styx.support.api.BlockingObservables.waitForResponse;
import static com.hotels.styx.support.api.matchers.HttpHeadersMatcher.isNotCacheable;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class PingHandlerTest {
    final PingHandler handler = new PingHandler();

    @Test
    public void respondsPongToPingRequest() {
        FullHttpResponse<String> response = waitForResponse(handler.handle(get("/ping").build()));
        assertThat(response.status(), is(OK));
        assertThat(response.headers(), isNotCacheable());
        assertThat(response.contentType().get(), is("text/plain; charset=utf-8"));
        assertThat(response.body(), is("pong"));
    }

}
