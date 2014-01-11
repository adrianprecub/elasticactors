/*
 * Copyright 2013 - 2014 The Original Authors
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

/**
 * @author Joost van de Wijgerd
 */
@Actor(stateClass = TestActorState.class,serializationFramework = TestSerializationFramework.class)
public class TestPersistentMethodActor extends MethodActor {

    public TestPersistentMethodActor() {
        super();
    }

    @MessageHandler
    public void handle(TestMessage message,ActorRef sender,TestActorState state,ActorSystem actorSystem) {
        System.out.println("handle called");
        state.setCallSucceeded(true);
        state.setActorSystem(actorSystem);
        state.setMessage(message);
        state.setSender(sender);
    }
}