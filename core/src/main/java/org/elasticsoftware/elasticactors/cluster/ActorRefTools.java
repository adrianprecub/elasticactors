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

package org.elasticsoftware.elasticactors.cluster;

import org.elasticsoftware.elasticactors.ActorRef;
import org.elasticsoftware.elasticactors.ActorSystems;

/**
 * @author Joost van de Wijgerd
 */
public final class ActorRefTools {
    private static final String EXCEPTION_FORMAT = "Invalid ActorRef, required spec: [actor://<cluster>/<actorSystem>/[shards|nodes|services]/<shardId>/<actorId (optional)>, actual spec: [%s]";

    private ActorRefTools() {}

    public static ActorRef parse(String refSpec, ActorSystems cluster) {
        // refSpec should look like: actor://<cluster>/<actorSystem>/shards/<shardId>/<actorId>
        if (refSpec.startsWith("actor://")) {
            int actorSeparatorIndex = 8;
            for (int i = 0; i < 3; i++) {
                int nextIndex = refSpec.indexOf('/', actorSeparatorIndex+1);
                if (nextIndex == -1) {
                    throw new IllegalArgumentException(
                            String.format(EXCEPTION_FORMAT, refSpec));
                } else {
                    actorSeparatorIndex = nextIndex;
                }
            }
            int nextIndex = refSpec.indexOf('/', actorSeparatorIndex+1);
            String actorId = (nextIndex == -1) ? null : refSpec.substring(nextIndex+1);
            actorSeparatorIndex = (nextIndex == -1) ? actorSeparatorIndex : nextIndex;
            String[] components = (actorId == null) ? refSpec.substring(8).split("/") : refSpec.substring(8, actorSeparatorIndex).split("/");

                String clusterName = components[0];
                if (!cluster.getClusterName().equals(clusterName)) {
                    throw new IllegalArgumentException(String.format("Cluster [%s] is not Local Cluster [%s]", cluster, clusterName));
                }
                String actorSystemName = components[1];
                InternalActorSystem actorSystem = (InternalActorSystem) cluster.get(actorSystemName);
                if (actorSystem == null) {
                    throw new IllegalArgumentException(String.format("Unknown ActorSystem: %s", actorSystemName));
                }
                if("shards".equals(components[2])) {
                    int shardId = Integer.parseInt(components[3]);
                    if (shardId >= actorSystem.getNumberOfShards()) {
                        throw new IllegalArgumentException(String.format("Unknown shard %d for ActorSystem %s. Available shards: %d", shardId, actorSystemName, actorSystem.getNumberOfShards()));
                    }
                    return new LocalClusterActorShardRef(clusterName, actorSystem.getShard(String.format("%s/shards/%d",actorSystemName,shardId)), actorId);
                } else if("nodes".equals(components[2])) {
                    return new LocalClusterActorNodeRef(clusterName,actorSystem.getNode(components[3]),actorId);
                } else if("services".equals(components[2])) {
                    return new ServiceActorRef(clusterName,actorSystem.getNode(),(actorId == null) ? components[3] : String.format("%s/%s",components[3],actorId));
                } else {
                    throw new IllegalArgumentException(String.format(EXCEPTION_FORMAT, refSpec));
                }

            } else {
                throw new IllegalArgumentException(String.format(EXCEPTION_FORMAT, refSpec));
            }

    }

    public static boolean isService(ActorRef ref) {
        return ref instanceof ServiceActorRef;
    }
}