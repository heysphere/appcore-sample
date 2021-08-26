package me.sphere.appcore.firestore

import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals

data class TestFirestoreTimestamp(override val seconds: Long, override val nanoseconds: Int): FirestoreTimestamp

class FirestoreDecodeTests {
    @Test
    fun test_timestamp_decoding() {
        val stub = mapOf(
            "name" to "Doge",
            "time" to TestFirestoreTimestamp(seconds = 1, nanoseconds = 2),
            "scoreboard" to mapOf(
                "itemA" to mapOf(
                    "subitem0" to listOf(1, 2, 3),
                    "subitem1" to listOf(4, 6, 8)
                ),
                "itemB" to listOf("ref0", "ref1")
            )
        )

        val decoded = firestoreDecode(TestData.serializer(), stub, StubDocumentReference)
        val expected = TestData(
            name = "Doge",
            scoreboard = TestScoreboard(
                itemA = mapOf(
                    "subitem0" to listOf(1, 2, 3),
                    "subitem1" to listOf(4, 6, 8)
                ),
                itemB = listOf("ref0", "ref1")
            ),
            time = TestTimestamp(1, 2)
        )

        assertEquals(decoded, expected)
    }
}

object StubDocumentReference: DocumentReference {
    override val documentID: String = "stub"
    override val path: String = "stubs/stub"

    override fun collection(id: String): CollectionReference = error("Unimplemented")
    override fun addSnapshotListenerForAppCore(includeMetadataChanges: Boolean, action: (DocumentSnapshot?, FirestoreException?) -> Unit) = error("Unimplemented")
    override fun get(action: (DocumentSnapshot?, FirestoreException?) -> Unit) = error("Unimplemented")
}

@Serializable
private data class TestData(val name: String, val scoreboard: TestScoreboard, val time: TestTimestamp)

@Serializable
private data class TestTimestamp(val seconds: Long, val nanoseconds: Int)

@Serializable
private data class TestScoreboard(val itemA: Map<String, List<Int>>, val itemB: List<String>)
