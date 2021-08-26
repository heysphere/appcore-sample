package me.sphere.appcore.firestore

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FirestoreMessageFilterTests {
    @Test
    fun testIsExclusiveFilterForNotRemoved() {
        assertFalse(
            FirestoreMessageFilter(onlyImportant = false, onlyNotRemoved = false)
                .isExclusiveFilterForNotRemoved
        )
        assertFalse(
            FirestoreMessageFilter(onlyImportant = true, onlyNotRemoved = false)
                .isExclusiveFilterForNotRemoved
        )
        assertFalse(
            FirestoreMessageFilter(onlyImportant = true, onlyNotRemoved = true)
                .isExclusiveFilterForNotRemoved
        )
        assertTrue(
            FirestoreMessageFilter(onlyImportant = false, onlyNotRemoved = true)
                .isExclusiveFilterForNotRemoved
        )
    }
}
