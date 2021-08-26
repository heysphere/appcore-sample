package me.sphere.appcore.firestore

import me.sphere.appcore.utils.freeze

class SphereFirestoreGatewayImpl: AbstractFirestoreGateway(), SphereFirestoreGateway {
    companion object {
        const val FEED_VERSION = 1
        const val FEED_VERSION_KEY = "feedVersion"
        const val ARCHIVED_KEY = "archived"

        const val SPHERES = "spheres"
        const val SPHERE_MEMBERSHIPS = "sphereMemberships"
        const val FEED = "feed"
    }

    init { freeze() }

    /**
     * Since we are listening to a large amount of documents, [QueryDelta] is more efficient than a plain [List]. This
     * is because we can avoid unchanged documents from being repeatedly deserialized, and we also use less transient
     * memory as a result.
     */
    override fun feed(sphereId: String, agentId: String) = flatMapLatestFirestore { firestore ->
        firestore
            .collection(SPHERES)
            .document(sphereId)
            .collection(SPHERE_MEMBERSHIPS)
            .document(agentId)
            .collection(FEED)
            .whereLessThanOrEqualTo(FEED_VERSION_KEY, FEED_VERSION) // obj[FEED_VERSION_KEY] <= FEED_VERSION
            .whereEqualTo(ARCHIVED_KEY, false)
            .listenDelta(FirestoreFeedItem.serializer())
    }
}
