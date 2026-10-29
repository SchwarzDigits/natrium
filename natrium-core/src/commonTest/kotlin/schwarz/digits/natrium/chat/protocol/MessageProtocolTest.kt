package schwarz.digits.natrium.chat.protocol

import kotlinx.datetime.Instant
import schwarz.digits.natrium.chat.MessageValue
import schwarz.digits.natrium.chat.Option
import schwarz.digits.natrium.chat.Request
import schwarz.digits.natrium.chat.Response
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MessageProtocolTest {

    private val options = listOf(Option("yes", "Yes"), Option("no", "No"))
    private val deadline = Instant.parse("2026-11-15T23:59:59Z")

    private fun roundTrip(value: MessageValue): MessageValue =
        MessageProtocol.decode(requireNotNull(MessageProtocol.encode(value)))

    @Test
    fun roundTripsEveryRequestType() {
        val requests = listOf(
            Request.SingleSelect("Residence", "Is this your main residence?", options, deadline),
            Request.MultiSelect(message = "Which documents do you have?", options = options),
            Request.Asset("Documents", "Please upload your ID"),
            Request.Appointment(
                message = "Appointment proposal",
                from = Instant.parse("2026-11-20T09:00:00Z"),
                to = Instant.parse("2026-11-20T09:30:00Z"),
            ),
        )
        requests.forEach { request ->
            assertEquals(MessageValue.RequestValue(request), roundTrip(MessageValue.RequestValue(request)))
        }
    }

    @Test
    fun roundTripsEveryResponseType() {
        val responses = listOf(
            Response.SingleSelect("yes"),
            Response.MultiSelect(listOf("yes", "no")),
            Response.Asset(listOf("file-1", "file-2")),
            Response.Appointment(accepted = true),
            Response.FreeText("Question back", listOf("file-3")),
        )
        responses.forEach { response ->
            val value = MessageValue.ResponseValue("request-1", response)
            assertEquals(value, roundTrip(value))
        }
    }

    @Test
    fun roundTripsTextWithAttachments() {
        val value = MessageValue.TextValue("Hello", listOf("file-1"))
        assertEquals(value, roundTrip(value))
    }

    @Test
    fun encodesCompactWithoutDefaults() {
        val json = MessageProtocol.encode(MessageValue.TextValue("Hi"))
        assertEquals("""{"natrium":1,"type":"TEXT","text":"Hi"}""", json)
    }

    @Test
    fun encodesResponseWireFormat() {
        val json = MessageProtocol.encode(MessageValue.ResponseValue("req-1", Response.SingleSelect("yes")))
        assertEquals(
            """{"natrium":1,"type":"RESPONSE","request_id":"req-1","response":{"type":"SINGLE_SELECT","selected":"yes"}}""",
            json,
        )
    }

    @Test
    fun decodesSpecExample() {
        val json = """
            {"natrium":1,"type":"REQUEST","request":{
              "type":"SINGLE_SELECT","subject":"Residence","message":"Main residence?",
              "options":[{"id":"yes","title":"Yes"},{"id":"no","title":"No"}],
              "deadline":"2026-11-15T23:59:59Z"
            }}
        """.trimIndent()
        assertEquals(
            MessageValue.RequestValue(Request.SingleSelect("Residence", "Main residence?", options, deadline)),
            MessageProtocol.decode(json),
        )
    }

    @Test
    fun plainTextStaysText() {
        assertEquals(MessageValue.TextValue("hello"), MessageProtocol.decode("hello"))
        assertEquals(MessageValue.TextValue("{not json"), MessageProtocol.decode("{not json"))
        assertEquals(MessageValue.TextValue("""{"foo":1}"""), MessageProtocol.decode("""{"foo":1}"""))
        assertEquals(MessageValue.TextValue("""{"natrium":"x"}"""), MessageProtocol.decode("""{"natrium":"x"}"""))
    }

    @Test
    fun newerVersionIsUnsupported() {
        assertEquals(
            MessageValue.UnsupportedValue(2, "TEXT"),
            MessageProtocol.decode("""{"natrium":2,"type":"TEXT","text":"Hi"}"""),
        )
    }

    @Test
    fun unknownTypesAreUnsupported() {
        assertEquals(
            MessageValue.UnsupportedValue(1, "POLL"),
            MessageProtocol.decode("""{"natrium":1,"type":"POLL"}"""),
        )
        assertEquals(
            MessageValue.UnsupportedValue(1, "REQUEST"),
            MessageProtocol.decode("""{"natrium":1,"type":"REQUEST","request":{"type":"PAYMENT","message":"x"}}"""),
        )
    }

    @Test
    fun malformedKnownTypeIsUnsupported() {
        assertEquals(
            MessageValue.UnsupportedValue(1, "RESPONSE"),
            MessageProtocol.decode("""{"natrium":1,"type":"RESPONSE","response":{"type":"SINGLE_SELECT","selected":"yes"}}"""),
        )
    }

    @Test
    fun ignoresUnknownFields() {
        assertEquals(
            MessageValue.TextValue("Hi"),
            MessageProtocol.decode("""{"natrium":1,"type":"TEXT","text":"Hi","future":true}"""),
        )
    }

    @Test
    fun previewsReadableText() {
        val request = MessageProtocol.encode(MessageValue.RequestValue(Request.Asset("Documents", "Please upload")))!!
        assertEquals("Documents", MessageProtocol.previewText(request))
        val untitled = MessageProtocol.encode(MessageValue.RequestValue(Request.Asset(message = "Please upload")))!!
        assertEquals("Please upload", MessageProtocol.previewText(untitled))
        val freeText = MessageProtocol.encode(MessageValue.ResponseValue("r", Response.FreeText("Why?")))!!
        assertEquals("Why?", MessageProtocol.previewText(freeText))
        assertNull(MessageProtocol.previewText("""{"natrium":2,"type":"TEXT"}"""))
        assertEquals("plain", MessageProtocol.previewText("plain"))
    }

    @Test
    fun locationIsNotTransportedAsText() {
        assertNull(MessageProtocol.encode(MessageValue.LocationValue(1f, 2f)))
        assertTrue(MessageProtocol.encode(MessageValue.TextValue("x"))!!.startsWith("{"))
    }
}
