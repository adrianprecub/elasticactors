/*
 * Copyright (c) 2013 Joost van de Wijgerd <jwijgerd@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elasterix.elasticactors.examples.petstore;

import org.elasterix.elasticactors.ActorState;
import org.elasterix.elasticactors.ActorSystemConfiguration;
import org.elasterix.elasticactors.serialization.Deserializer;
import org.elasterix.elasticactors.serialization.MessageDeserializer;
import org.elasterix.elasticactors.serialization.MessageSerializer;
import org.elasterix.elasticactors.serialization.Serializer;

/**
 *
 */
public class Petstore implements ActorSystemConfiguration {
    @Override
    public String getName() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int getNumberOfShards() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public <T> MessageSerializer<T> getSerializer(Class<T> messageClass) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public <T> MessageDeserializer<T> getDeserializer(Class<T> messageClass) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Serializer<ActorState, byte[]> getActorStateSerializer() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Deserializer<byte[], ActorState> getActorStateDeserializer() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}