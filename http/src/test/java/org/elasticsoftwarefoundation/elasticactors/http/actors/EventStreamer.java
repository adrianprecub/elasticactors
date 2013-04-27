/*
 * Copyright 2013 Joost van de Wijgerd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elasticsoftwarefoundation.elasticactors.http.actors;

import com.google.common.base.Charsets;
import org.apache.log4j.Logger;
import org.elasterix.elasticactors.ActorRef;
import org.elasterix.elasticactors.TypedActor;
import org.elasterix.elasticactors.UntypedActor;
import org.elasticsoftwarefoundation.elasticactors.http.messages.*;
import org.jboss.netty.handler.codec.http.HttpHeaders;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author Joost van de Wijgerd
 */
public final class EventStreamer extends UntypedActor {
    private static final Logger logger = Logger.getLogger(EventStreamer.class);

    @Override
    public void postActivate(String previousVersion) throws Exception {
        // register ourselves with the http server
        ActorRef httpServer = getSystem().getParent().get("Http").serviceActorFor("httpServer");
        httpServer.tell(new RegisterRouteMessage(String.format("/%s", getSelf().getActorId()),getSelf()),getSelf());
    }

    @Override
    public void onReceive(ActorRef sender, Object message) throws Exception {
        if(message instanceof HttpRequest) {
            handle(sender,(HttpRequest) message);
        } else if(message instanceof String) {
            sender.tell(new ServerSentEvent("testing","event",Arrays.asList((String)message),null),getSelf());
            getSystem().getScheduler().scheduleOnce(sender,("Ping".equals(message)) ? "Pong" : "Ping",getSelf(),10, TimeUnit.SECONDS);
        }
    }

    private void handle(ActorRef sender,HttpRequest message) {
        logger.info("Got request");
        // start streaming events
        sender.tell(new SseResponse(),getSelf());
        // schedule the next event
        getSystem().getScheduler().scheduleOnce(sender,"Ping",getSelf(),10, TimeUnit.SECONDS);
    }
}