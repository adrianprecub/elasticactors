/*
 * Copyright 2013 the original authors
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

package org.elasticsoftware.elasticactors;

import org.elasticsoftware.elasticactors.serialization.Deserializer;
import org.elasticsoftware.elasticactors.serialization.MessageDeserializer;
import org.elasticsoftware.elasticactors.serialization.MessageSerializer;
import org.elasticsoftware.elasticactors.serialization.Serializer;

import java.util.Set;

/**
 * @author Joost van de Wijgerd
 */
public interface ActorSystemConfiguration {
    /**
     * The name of this {@link ActorSystem}. The name has to be unique within the same cluster
     *
     * @return
     */
    String getName();

    /**
     * The number of shards. This determines how big an {@link org.elasticsoftware.elasticactors.ActorSystem} can scale. If a cluster
     * contains more nodes than shards then not every node will have a shard.
     *
     * @return
     */
    int getNumberOfShards();

    String getVersion();

    <T> MessageSerializer<T> getSerializer(Class<T> messageClass);

    <T> MessageDeserializer<T> getDeserializer(Class<T> messageClass);

    Serializer<ActorState, byte[]> getActorStateSerializer();

    /**
     * Pluggable {@link org.elasticsoftware.elasticactors.serialization.Deserializer} for the internal state of the actor
     *
     * @return the {@link org.elasticsoftware.elasticactors.serialization.Deserializer} used to deserialize from a byte array to an {@link ActorState} instance
     */
    Deserializer<byte[], ActorState> getActorStateDeserializer();

    ActorStateFactory getActorStateFactory();

    ElasticActor getService(String serviceId);

    /**
     * Returns all services that this configuration defines. Used to initialize them properly
     *
     * @return
     */
    Set<String> getServices();
}