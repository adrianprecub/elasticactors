/*
 * Copyright 2013 - 2017 The Original Authors
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

package org.elasticsoftware.elasticactors.reactivestreams;

import org.elasticsoftware.elasticactors.ActorRef;
import org.elasticsoftware.elasticactors.messaging.reactivestreams.CancelMessage;
import org.elasticsoftware.elasticactors.messaging.reactivestreams.RequestMessage;

import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * @author Joost van de Wijgerd
 */
public final class PersistentSubscriptionImpl implements InternalPersistentSubscription {
    private final ActorRef subscriberRef;
    private final ActorRef publisherRef;
    private final String messageName;
    private final AtomicBoolean cancelled;
    @Nullable
    private final transient Consumer<ActorRef> undeliverableFunction;

    public PersistentSubscriptionImpl(ActorRef subscriberRef, ActorRef publisherRef, String messageName) {
        this(subscriberRef, publisherRef, messageName, false, null);
    }

    public PersistentSubscriptionImpl(ActorRef subscriberRef, ActorRef publisherRef, String messageName, @Nullable Consumer<ActorRef> undeliverableFunction) {
        this(subscriberRef, publisherRef, messageName, false, undeliverableFunction);

    }

    public PersistentSubscriptionImpl(ActorRef subscriberRef, ActorRef publisherRef, String messageName, boolean cancelled) {
        this(subscriberRef, publisherRef, messageName, cancelled, null);
    }

    private PersistentSubscriptionImpl(ActorRef subscriberRef, ActorRef publisherRef, String messageName, boolean cancelled, Consumer<ActorRef> undeliverableFunction) {
        this.subscriberRef = subscriberRef;
        this.publisherRef = publisherRef;
        this.messageName = messageName;
        this.cancelled = new AtomicBoolean(cancelled);
        this.undeliverableFunction = undeliverableFunction;
    }

    @Override
    public ActorRef getPublisherRef() {
        return publisherRef;
    }

    @Override
    public String getMessageName() {
        return messageName;
    }

    @Override
    public boolean isCancelled() {
        return cancelled.get();
    }

    @Override
    public void request(long n) {
        if(!cancelled.get()) {
            publisherRef.tell(new RequestMessage(messageName, n), subscriberRef);
        }
    }

    @Override
    public void cancel() {
        if(cancelled.compareAndSet(false, true)) {
            publisherRef.tell(new CancelMessage(subscriberRef, messageName), subscriberRef);
        }
    }

    @Override
    @Nullable
    public Consumer<ActorRef> getUndeliverableFunction() {
        return undeliverableFunction;
    }
}
