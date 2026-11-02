package schwarz.digits.natrium.chat.protocol

import kotlinx.datetime.Instant
import schwarz.digits.natrium.chat.AppointmentSlot
import schwarz.digits.natrium.chat.ChatMessage
import schwarz.digits.natrium.chat.InvalidResponseReason
import schwarz.digits.natrium.chat.MessageStatus
import schwarz.digits.natrium.chat.MessageValue
import schwarz.digits.natrium.chat.Option
import schwarz.digits.natrium.chat.Request
import schwarz.digits.natrium.chat.Response
import schwarz.digits.natrium.users.NamedUser
import schwarz.digits.natrium.users.UserId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ResponseRulesTest {

    private val options = listOf(Option("a", "A"), Option("b", "B"))
    private val single = Request.SingleSelect(message = "?", options = options)
    private val multi = Request.MultiSelect(message = "?", options = options)
    private val asset = Request.Asset(message = "?")
    private val appointment = Request.Appointment(
        message = "?",
        slots = listOf(
            AppointmentSlot("s1", Instant.parse("2026-11-20T09:00:00Z"), Instant.parse("2026-11-20T09:30:00Z")),
            AppointmentSlot("s2", Instant.parse("2026-11-21T14:00:00Z"), Instant.parse("2026-11-21T14:30:00Z")),
        ),
    )
    private val acknowledge = Request.Acknowledge(message = "?", confirmation = "I have read it.")
    private val freeText = Request.FreeText(message = "?", maxLength = 10)

    @Test
    fun freeTextIsValidForEveryRequest() {
        listOf(single, multi, asset, appointment, acknowledge, freeText).forEach {
            assertNull(ResponseRules.validate(it, Response.FreeText("question")))
        }
    }

    @Test
    fun singleSelect() {
        assertNull(ResponseRules.validate(single, Response.SingleSelect("a")))
        assertEquals(InvalidResponseReason.UNKNOWN_OPTION, ResponseRules.validate(single, Response.SingleSelect("x")))
        assertEquals(InvalidResponseReason.TYPE_MISMATCH, ResponseRules.validate(single, Response.MultiSelect(listOf("a"))))
    }

    @Test
    fun multiSelect() {
        assertNull(ResponseRules.validate(multi, Response.MultiSelect(listOf("a", "b"))))
        assertEquals(InvalidResponseReason.EMPTY_SELECTION, ResponseRules.validate(multi, Response.MultiSelect(emptyList())))
        assertEquals(InvalidResponseReason.UNKNOWN_OPTION, ResponseRules.validate(multi, Response.MultiSelect(listOf("a", "x"))))
        assertEquals(InvalidResponseReason.UNKNOWN_OPTION, ResponseRules.validate(multi, Response.MultiSelect(listOf("a", "a"))))
        assertEquals(InvalidResponseReason.TYPE_MISMATCH, ResponseRules.validate(multi, Response.SingleSelect("a")))
    }

    @Test
    fun assetNeedsAttachments() {
        assertNull(ResponseRules.validate(asset, Response.Asset(listOf("file"))))
        assertNull(ResponseRules.validate(asset, Response.Asset(), pendingAttachments = 1))
        assertEquals(InvalidResponseReason.MISSING_ATTACHMENTS, ResponseRules.validate(asset, Response.Asset()))
        assertEquals(InvalidResponseReason.TYPE_MISMATCH, ResponseRules.validate(asset, Response.Appointment("s1")))
    }

    @Test
    fun appointment() {
        assertNull(ResponseRules.validate(appointment, Response.Appointment("s2")))
        assertEquals(InvalidResponseReason.UNKNOWN_OPTION, ResponseRules.validate(appointment, Response.Appointment("x")))
        assertEquals(InvalidResponseReason.TYPE_MISMATCH, ResponseRules.validate(appointment, Response.SingleSelect("a")))
    }

    @Test
    fun acknowledge() {
        assertNull(ResponseRules.validate(acknowledge, Response.Acknowledge))
        assertEquals(InvalidResponseReason.TYPE_MISMATCH, ResponseRules.validate(acknowledge, Response.SingleSelect("a")))
    }

    @Test
    fun freeTextRequestOnlyTakesFreeTextWithinMaxLength() {
        assertNull(ResponseRules.validate(freeText, Response.FreeText("1234567890")))
        assertEquals(InvalidResponseReason.TEXT_TOO_LONG, ResponseRules.validate(freeText, Response.FreeText("12345678901")))
        assertEquals(InvalidResponseReason.TYPE_MISMATCH, ResponseRules.validate(freeText, Response.Acknowledge))
    }

    @Test
    fun answeredStatePointsToFirstSentResponse() {
        val messages = listOf(
            message("req", MessageValue.RequestValue(single), second = 1),
            message("failed", MessageValue.ResponseValue("req", Response.SingleSelect("a")), second = 2, status = MessageStatus.FAILED),
            message("first", MessageValue.ResponseValue("req", Response.FreeText("?")), second = 3),
            message("second", MessageValue.ResponseValue("req", Response.SingleSelect("b")), second = 4),
            message("open", MessageValue.RequestValue(asset), second = 5),
        )

        val result = ResponseRules.withAnsweredState(messages).associateBy { it.id }

        assertEquals("first", result.getValue("req").answeredByMessageId)
        assertNull(result.getValue("open").answeredByMessageId)
        assertNull(result.getValue("first").answeredByMessageId)
        assertEquals("first", ResponseRules.firstResponseId("req", messages))
        assertNull(ResponseRules.firstResponseId("open", messages))
    }

    private fun message(
        id: String,
        value: MessageValue,
        second: Int,
        status: MessageStatus = MessageStatus.SENT,
    ) = ChatMessage(
        id = id,
        sender = NamedUser(UserId.fromString("user@example.com"), "User"),
        value = value,
        timestamp = Instant.fromEpochSeconds(second.toLong()),
        status = status,
        isSelf = false,
    )
}
