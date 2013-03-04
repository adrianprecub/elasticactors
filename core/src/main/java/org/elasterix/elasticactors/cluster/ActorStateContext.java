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

package org.elasterix.elasticactors.cluster;

import org.elasterix.elasticactors.ActorState;
import org.elasterix.elasticactors.state.ActorStateMap;

import java.util.HashMap;

/**
 * @author Joost van de Wijgerd
 */
public class ActorStateContext {
    private static final ThreadLocal<ActorState> threadContext = new ThreadLocal<ActorState>();

    protected static ActorState setState(ActorState context) {
        final ActorState currentContext = threadContext.get();
        threadContext.set(context);
        return currentContext;
    }

    public static ActorState getState() {
        ActorState state =  threadContext.get();
        if(state == null) {
            state = new ActorStateMap(new HashMap<String,Object>());
            threadContext.set(state);
        }
        return state;
    }

    protected static ActorState getAndClearState() {
        ActorState state = threadContext.get();
        threadContext.set(null);
        return state;
    }
}