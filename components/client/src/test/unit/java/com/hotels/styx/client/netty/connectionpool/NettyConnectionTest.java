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
package com.hotels.styx.client.netty.connectionpool;

import com.hotels.styx.api.client.Connection;
import com.hotels.styx.api.client.Origin;
import com.hotels.styx.support.server.FakeHttpServer;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.hotels.styx.support.api.BlockingObservables.getFirst;
import static com.hotels.styx.api.client.Origin.newOriginBuilder;
import static com.hotels.styx.support.matchers.IsOptional.isAbsent;
import static com.hotels.styx.support.matchers.IsOptional.isValue;
import static com.hotels.styx.api.support.HostAndPorts.localHostAndFreePort;
import static com.hotels.styx.client.connectionpool.ConnectionPoolSettings.defaultSettableConnectionPoolSettings;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class NettyConnectionTest {
    private final NettyConnectionFactory connectionFactory = new NettyConnectionFactory.Builder().build();
    private final Origin origin = newOriginBuilder(localHostAndFreePort()).build();
    private final FakeHttpServer originServer = new FakeHttpServer(origin.host().getPort());

    @BeforeMethod
    public void startOriginServer() {
        originServer.start();
    }

    @AfterMethod
    public void shutdownServer() {
        originServer.stop();
    }

    @Test
    public void willNotNotifyListenersIfConnectionRemainActive() throws Exception {
        Connection connection = createConnection();
        EventCapturingListener listener = new EventCapturingListener();
        connection.addConnectionListener(listener);

        assertThat(listener.closedConnection(), isAbsent());
    }

    @Test
    public void notifiesListenersWhenConnectionIsClosed() throws Exception {
        Connection connection = createConnection();

        EventCapturingListener listener = new EventCapturingListener();
        connection.addConnectionListener(listener);

        originServer.stop();
        listener.waitForEvent();

        assertThat(connection.isConnected(), is(false));
        assertThat(listener.closedConnection(), isValue(connection));
    }

    private Connection createConnection() {
        return getFirst(connectionFactory.createConnection(origin, defaultSettableConnectionPoolSettings()));
    }

    static class EventCapturingListener implements Connection.Listener {
        private Connection closedConnection;
        private final CountDownLatch latch = new CountDownLatch(1);

        @Override
        public void connectionClosed(Connection connection) {
            this.closedConnection = connection;
            this.latch.countDown();
        }

        Optional<Connection> closedConnection() {
            return Optional.ofNullable(this.closedConnection);
        }

        void waitForEvent() throws InterruptedException {
            latch.await(3, TimeUnit.SECONDS);
        }
    }
}
