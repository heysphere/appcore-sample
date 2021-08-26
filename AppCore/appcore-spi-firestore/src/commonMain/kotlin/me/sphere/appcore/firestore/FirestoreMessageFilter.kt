package me.sphere.appcore.firestore

data class FirestoreMessageFilter(
    val onlyImportant: Boolean = false,
    val onlyNotRemoved: Boolean = false
) {
    /** IMPORTANT: If more combinations are added, this should be updated to reflect that ONLY [onlyNotRemoved] is true. */
    val isExclusiveFilterForNotRemoved get() = !onlyImportant && onlyNotRemoved

    companion object {
        val None get() = FirestoreMessageFilter()
    }
}
