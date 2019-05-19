/*
 * Copyright 2015-2019 Austin Keener, Michael Ritter, Florian Spieß, and the JDA contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.dv8tion.jda.api.utils;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.internal.utils.tuple.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Controls states and behaviour of one or multiple {@link net.dv8tion.jda.api.JDA JDA} instances.
 * <br>One instance of this should be used when sharding a bot account in order to keep track of session information
 * between shards.
 *
 * <p>The {@link net.dv8tion.jda.api.utils.SessionControllerAdapter SessionControllerAdapter} provides
 * a default implementation that can be extended and overridden.
 *
 * <h2>States {@literal &} Behaviour</h2>
 * <b>Identify Ratelimit Handling</b>
 * <br>This will enable handling of (re-)connecting gateway sessions.
 *
 * <p><b>Global REST Ratelimit</b>
 * <br>The global REST ratelimit is not bound to a single session and should be
 * handled on all JDA instances. This controller will receive updates of this ratelimit through {@link #setGlobalRatelimit(long)}
 * and should report the last ratelimit information it received through {@link #getGlobalRatelimit()}.
 *
 * <p><b>Gateway Provider</b>
 * <br>This provider can be used to change the gateway retrieval (using cache, http, or static) and
 * allows to set a custom gateway endpoint. <b>Use carefully.</b>
 *
 * <h2>Examples</h2>
 *
 * <b>Using {@link net.dv8tion.jda.api.JDABuilder JDABuilder}</b>
 * <br>
 * <pre><code>
 * JDABuilder builder = new JDABuilder(AccountType.BOT).setToken(BOT_TOKEN);
 * builder.setSessionController(new SessionControllerAdapter() {
 *     {@literal @Override}
 *     public void appendSession(SessionConnectNode node) {
 *         System.out.println("[SessionController] Adding SessionConnectNode to Queue!");
 *         super.appendSession(node);
 *     }
 * });
 * builder.addEventListeners(myListener);
 * for (int i = 0; i {@literal <} 10; i++) {
 *     builder.useSharding(i, 10).buildAsync();
 * }
 * </code></pre>
 *
 * <p><b>Using {@link net.dv8tion.jda.api.sharding.ShardManager ShardManager}</b>
 * <br>
 * <pre><code>
 * DefaultShardManagerBuilder builder = new DefaultShardManagerBuilder();
 * builder.setToken(BOT_TOKEN);
 * builder.setSessionController(new SessionControllerAdapter() {
 *     {@literal @Override}
 *     public {@literal Pair<String, Integer>} getGatewayBot(JDA api) {
 *         return Pair.of(getGateway(), 10);
 *     }
 * });
 * builder.addEventListeners(myListener);
 * builder.build();
 * </code></pre>
 */
public interface SessionController
{
    /**
     * The default delay (in seconds) to wait between running {@link net.dv8tion.jda.api.utils.SessionController.SessionConnectNode SessionConnectNodes}
     */
    int IDENTIFY_DELAY = 5;

    /**
     * Called by {@link net.dv8tion.jda.internal.requests.WebSocketClient WebSocketClient} when
     * a WebSocket should be started. (Connecting and Reconnecting)
     * <br>This should only add the node to a queue and execute the queue with respect to the {@link #IDENTIFY_DELAY}.
     *
     * @param  node
     *         The {@link net.dv8tion.jda.api.utils.SessionController.SessionConnectNode SessionConnectNode}
     */
    void appendSession(@Nonnull SessionConnectNode node);

    /**
     * Called by {@link net.dv8tion.jda.internal.requests.WebSocketClient WebSocketClient} when
     * a JDA session has been shutdown.
     * <br>When this happened the {@link net.dv8tion.jda.api.utils.SessionController.SessionConnectNode#run(boolean) SessionConnectNode.run(boolean)}
     * will be a no-op and does not contribute to the {@link #IDENTIFY_DELAY}.
     *
     * @param node
     *        The {@link net.dv8tion.jda.api.utils.SessionController.SessionConnectNode SessionConnectNode} to remove from the queue.
     */
    void removeSession(@Nonnull SessionConnectNode node);

    /**
     * Provides the cross-session global REST ratelimit it received through {@link #setGlobalRatelimit(long)}.
     *
     * @return The current global REST ratelimit or -1 if unset
     */
    long getGlobalRatelimit();

    /**
     * Called by the {@link net.dv8tion.jda.internal.requests.RateLimiter RateLimiter} if the global rest ratelimit
     * has changed.
     *
     * @param ratelimit
     *        The new global ratelimit
     */
    void setGlobalRatelimit(long ratelimit);

    /**
     * Called by {@link net.dv8tion.jda.internal.requests.WebSocketClient WebSocketClient}
     * when a new session starts (Connecting, Reconnecting).
     * <br>Should provide the gateway endpoint (wss) to connect to.
     *
     * @param  api
     *         The current JDA instance (used for RestActions and ShardInfo)
     *
     * @return The gateway endpoint
     */
    @Nonnull
    String getGateway(@Nonnull JDA api);

    /**
     * Called by {@link net.dv8tion.jda.api.sharding.DefaultShardManager DefaultShardManager}
     * when a new shards is starting.
     * <br>Should provide a {@link net.dv8tion.jda.internal.utils.tuple.Pair Pair} with {@code (gateway, shardTotal)}.
     *
     * @param  api
     *         The current JDA instance (used for RestActions and ShardInfo)
     *
     * @return The Pair consisting of the gateway endpoint to connect to and the shardTotal
     *
     * @see    #getGateway(net.dv8tion.jda.api.JDA)
     */
    @Nonnull
    Pair<String, Integer> getGatewayBot(@Nonnull JDA api);

    /**
     * Represents a WebSocketClient request to start a session.
     * <br>Not implemented by library user.
     *
     * <p><b>Note: None of the provided session nodes can be resumed, the resume timeframe has already passed</b>
     */
    interface SessionConnectNode
    {
        /**
         * Whether this node is reconnecting. Can be used to setup a priority based system.
         *
         * @return True, if this session is reconnecting
         */
        boolean isReconnect();

        /**
         * The {@link net.dv8tion.jda.api.JDA JDA} instance for this request
         *
         * @return The JDA instance
         */
        @Nonnull
        JDA getJDA();

        /**
         * The {@link net.dv8tion.jda.api.JDA.ShardInfo ShardInfo} for this request.
         * <br>Can be used for a priority system.
         *
         * @return The ShardInfo
         */
        @Nullable
        JDA.ShardInfo getShardInfo();

        /**
         * When called, establishes the session.
         * <br>This will return once the required payload to start the session has been delivered.
         *
         * @param  isLast
         *         True, if this is the last node in a queue worker.
         *         When false this will not wait for the payload to be delivered.
         *
         * @throws InterruptedException
         *         If the calling thread is interrupted
         */
        void run(boolean isLast) throws InterruptedException;
    }
}
