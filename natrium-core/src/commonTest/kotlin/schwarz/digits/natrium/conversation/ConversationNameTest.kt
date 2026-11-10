package schwarz.digits.natrium.conversation

import kotlin.test.Test
import kotlin.test.assertEquals

class ConversationNameTest {

    @Test
    fun plainTitleWithoutMetadata() {
        assertEquals("Team", ConversationName.encode("Team", emptyMap()))
        assertEquals(ConversationName.Decoded("Team", emptyMap()), ConversationName.decode("Team"))
    }

    @Test
    fun envelopeWithMetadata() {
        val name = ConversationName.encode("Authority · BA-1", mapOf("case_number" to "BA-1", "ref" to "x"))
        assertEquals("""{"natrium":1,"title":"Authority · BA-1","meta":{"case_number":"BA-1","ref":"x"}}""", name)
        assertEquals(
            ConversationName.Decoded("Authority · BA-1", mapOf("case_number" to "BA-1", "ref" to "x")),
            ConversationName.decode(name),
        )
    }

    @Test
    fun emptyTitleIsOmitted() {
        assertEquals("""{"natrium":1,"meta":{"k":"v"}}""", ConversationName.encode("", mapOf("k" to "v")))
        assertEquals(ConversationName.Decoded("", mapOf("k" to "v")), ConversationName.decode("""{"natrium":1,"meta":{"k":"v"}}"""))
    }

    @Test
    fun foreignJsonAndNewerVersionsStayPlain() {
        listOf("""{"case_number":"BA-1"}""", """{"natrium":2,"title":"x"}""", "{broken", """{"natrium":"1"}""").forEach {
            assertEquals(ConversationName.Decoded(it, emptyMap()), ConversationName.decode(it))
        }
    }

    @Test
    fun nonStringMetadataValuesAreDropped() {
        assertEquals(
            ConversationName.Decoded("t", mapOf("a" to "1")),
            ConversationName.decode("""{"natrium":1,"title":"t","meta":{"a":"1","b":2,"c":null,"d":{}}}"""),
        )
    }
}
