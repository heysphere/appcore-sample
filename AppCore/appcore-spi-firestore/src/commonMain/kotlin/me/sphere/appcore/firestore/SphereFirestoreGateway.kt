package me.sphere.appcore.firestore

import kotlinx.coroutines.flow.Flow

interface SphereFirestoreGateway {
    /**
     * Listen to the feed item collection.
     *
     * Note that all items go into [QueryDelta.inserted] in the first [QueryDelta] delivery.
     */
    fun feed(sphereId: String, agentId: String): Flow<QueryDelta<FirestoreFeedItem>>
}
