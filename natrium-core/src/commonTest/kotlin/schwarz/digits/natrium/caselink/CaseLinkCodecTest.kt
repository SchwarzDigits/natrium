package schwarz.digits.natrium.caselink

import schwarz.digits.natrium.users.UserId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CaseLinkCodecTest {

    private val user = UserId("35a9c68b-e97d-43b0-9dc1-0f76e7cf5a30", "example.org")

    @Test
    fun encodesBaseFragmentAndUrlSafeToken() {
        val url = CaseLinkCodec.encode("https://authority.example/import", CaseLinkCodec.Payload(user, "r-1"))
        assertEquals(
            "https://authority.example/import#" +
                "eyJ2IjoxLCJ1c2VyIjoiMzVhOWM2OGItZTk3ZC00M2IwLTlkYzEtMGY3NmU3Y2Y1YTMwQGV4YW1wbGUub3JnIiwicmVmIjoici0xIn0",
            url,
        )
        val token = url.substringAfter('#')
        assertTrue(token.none { it == '+' || it == '/' || it == '=' })
    }

    @Test
    fun roundTripsLinkAndBareToken() {
        val url = CaseLinkCodec.encode("https://a.example/import", CaseLinkCodec.Payload(user, "r-2"))
        assertEquals(CaseLinkCodec.Payload(user, "r-2"), CaseLinkCodec.decode(url))
        assertEquals(CaseLinkCodec.Payload(user, "r-2"), CaseLinkCodec.decode(url.substringAfter('#')))
    }

    @Test
    fun rejectsForeignInput() {
        listOf("", "https://a.example/import", "#not-base64!", "e30", "eyJ2IjoyLCJ1c2VyIjoiYUBiIiwicmVmIjoiciJ9")
            .forEach { assertNull(CaseLinkCodec.decode(it), it) }
    }

    @Test
    fun refsAreUnique() {
        assertNotEquals(CaseLinkCodec.newRef(), CaseLinkCodec.newRef())
        assertEquals(36, CaseLinkCodec.newRef().length)
    }
}
