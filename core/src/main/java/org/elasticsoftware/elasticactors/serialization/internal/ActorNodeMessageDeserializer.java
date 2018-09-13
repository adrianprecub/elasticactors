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

package org.elasticsoftware.elasticactors.serialization.internal;

import com.google.protobuf.ByteString;
import org.elasticsoftware.elasticactors.ActorRef;
import org.elasticsoftware.elasticactors.cluster.MessageSerializationRegistry;
import org.elasticsoftware.elasticactors.messaging.internal.ActorNodeMessage;
import org.elasticsoftware.elasticactors.serialization.MessageDeserializer;
import org.elasticsoftware.elasticactors.serialization.protobuf.Elasticactors;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author Joost van de Wijgerd
 */
public final class ActorNodeMessageDeserializer implements MessageDeserializer<ActorNodeMessage> {
    private final ActorRefDeserializer actorRefDeserializer;
    private final MessageSerializationRegistry messageSerializationRegistry;

    public ActorNodeMessageDeserializer(ActorRefDeserializer actorRefDeserializer, MessageSerializationRegistry messageSerializationRegistry) {
        this.actorRefDeserializer = actorRefDeserializer;
        this.messageSerializationRegistry = messageSerializationRegistry;
    }

    @Override
    public ActorNodeMessage deserialize(ByteBuffer serializedObject) throws IOException {
        try {
            Elasticactors.ActorNodeMessage protobufMessage = Elasticactors.ActorNodeMessage.parseFrom(ByteString.copyFrom(serializedObject));
            ActorRef receiverRef = protobufMessage.getReceiver()!=null && !protobufMessage.getReceiver().isEmpty() ? actorRefDeserializer.deserialize(protobufMessage.getReceiver()) : null;
            String messageClassString = protobufMessage.getPayloadClass();
            Class<?> messageClass = Class.forName(messageClassString);
            Object payloadObject = messageSerializationRegistry.getDeserializer(messageClass).deserialize(protobufMessage.getPayload().asReadOnlyByteBuffer());
            return new ActorNodeMessage(protobufMessage.getNodeId(), receiverRef, payloadObject, protobufMessage.getUndeliverable());
        } catch(Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    public Class<ActorNodeMessage> getMessageClass() {
        return ActorNodeMessage.class;
    }
}
