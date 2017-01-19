/*
 * Copyright 2013 - 2016 The Original Authors
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

package org.elasticsoftware.elasticactors.messaging;

import com.google.common.collect.ImmutableList;
import org.elasticsoftware.elasticactors.ActorRef;
import org.elasticsoftware.elasticactors.serialization.MessageDeliveryMode;
import org.elasticsoftware.elasticactors.serialization.MessageDeserializer;
import org.elasticsoftware.elasticactors.serialization.SerializationContext;
import org.elasticsoftware.elasticactors.serialization.internal.InternalMessageSerializer;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.UUID;

import static org.elasticsoftware.elasticactors.messaging.UUIDTools.createTimeBasedUUID;

/**
 * @author Joost van de Wijgerd
 */
public final class InternalMessageImpl implements InternalMessage,Serializable {
    private final ActorRef sender;
    private final ImmutableList<ActorRef> receivers;
    private final UUID id;
    private final ByteBuffer payload;
    private final String payloadClass;
    private final boolean durable;
    private final boolean undeliverable;
    private final MessageDeliveryMode deliveryMode;
    private final int timeout;
    private transient byte[] serializedForm;

    public InternalMessageImpl(ActorRef sender, ActorRef receiver, ByteBuffer payload, String payloadClass, boolean durable, boolean undeliverable, int timeout, MessageDeliveryMode deliveryMode) {
        this(sender, ImmutableList.of(receiver), payload, payloadClass, durable, undeliverable, timeout, deliveryMode);
    }

    public InternalMessageImpl(ActorRef sender, ImmutableList<ActorRef> receivers, ByteBuffer payload, String payloadClass, boolean durable, int timeout, MessageDeliveryMode deliveryMode) {
        this(sender, receivers, payload, payloadClass, durable, false, timeout, deliveryMode);
    }

    public InternalMessageImpl(ActorRef sender, ImmutableList<ActorRef> receivers, ByteBuffer payload, String payloadClass, boolean durable, boolean undeliverable, int timeout, MessageDeliveryMode deliveryMode) {
        this(createTimeBasedUUID(), sender, receivers, payload, payloadClass, durable, undeliverable, timeout, deliveryMode);
    }

    public InternalMessageImpl(UUID id, ActorRef sender, ImmutableList<ActorRef> receivers, ByteBuffer payload, String payloadClass, boolean durable, int timeout, MessageDeliveryMode deliveryMode) {
        this(id, sender, receivers, payload, payloadClass, durable, false, timeout, deliveryMode);
    }

    public InternalMessageImpl(UUID id,
                               ActorRef sender,
                               ImmutableList<ActorRef> receivers,
                               ByteBuffer payload,
                               String payloadClass,
                               boolean durable,
                               boolean undeliverable,
                               int timeout,
                               MessageDeliveryMode deliveryMode) {
        this.sender = sender;
        this.receivers = receivers;
        this.id = id;
        this.payload = payload;
        this.payloadClass = payloadClass;
        this.durable = durable;
        this.undeliverable = undeliverable;
        this.deliveryMode = deliveryMode;
        this.timeout = timeout;
    }

    public ActorRef getSender() {
        return sender;
    }

    @Override
    public ImmutableList<ActorRef> getReceivers() {
        return receivers;
    }

    public UUID getId() {
        return id;
    }

    public ByteBuffer getPayload() {
        return payload;
    }

    @Override
    public <T> T getPayload(MessageDeserializer<T> deserializer) throws IOException {
        //return deserializer.deserialize(payload);
        return SerializationContext.deserialize(deserializer, payload);
    }

    @Override
    public String getPayloadClass() {
        return payloadClass;
    }

    @Override
    public boolean isDurable() {
        return durable;
    }

    @Override
    public boolean isUndeliverable() {
        return undeliverable;
    }

    @Override
    public int getTimeout() {
        return timeout;
    }

    @Override
    public MessageDeliveryMode getDeliveryMode() {
        return deliveryMode;
    }

    @Override
    public byte[] toByteArray() {
        if(serializedForm == null) {
            serializedForm = InternalMessageSerializer.get().serialize(this);
        }
        return serializedForm;
    }

    @Override
    public InternalMessage copyOf() {
        return new InternalMessageImpl(id, sender, receivers, payload.asReadOnlyBuffer(), payloadClass, durable, undeliverable, timeout, deliveryMode);
    }
}
