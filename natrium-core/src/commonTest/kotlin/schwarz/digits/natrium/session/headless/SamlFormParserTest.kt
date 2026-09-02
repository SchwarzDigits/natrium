/*
 * Copyright (C) 2026 Schwarz Digits KG
 *
 * Licensed under the EUPL v. 1.2 only.
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 *
 * SPDX-License-Identifier: EUPL-1.2
 */

package schwarz.digits.natrium.session.headless

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SamlFormParserTest {

    @Test
    fun parsesActionMethodAndHiddenFieldsWithUnescaping() {
        val html = """
            <html><body onload="document.forms[0].submit()">
              <form method="POST" action="https://sp.example.com/sso/finalize-login?x=1&amp;y=2">
                <input type="hidden" name="SAMLResponse" value="PHNhbWxw+Base/64==" />
                <input type="hidden" name="RelayState" value="a&amp;b" />
                <input type="submit" value="Continue" />
              </form>
            </body></html>
        """.trimIndent()

        val form = SamlFormParser.parse(html)

        assertNotNull(form)
        assertEquals("https://sp.example.com/sso/finalize-login?x=1&y=2", form.action)
        assertEquals(HeadlessSsoMethod.POST, form.method)
        // base64 value with + / = survives inside quotes; the un-named submit button is ignored.
        assertEquals("PHNhbWxw+Base/64==", form.fields["SAMLResponse"])
        assertEquals("a&b", form.fields["RelayState"])
        assertEquals(2, form.fields.size)
    }

    @Test
    fun returnsNullWhenNoForm() {
        assertNull(SamlFormParser.parse("<html><body>nothing to submit</body></html>"))
    }

    @Test
    fun defaultsToPostAndReadsUnquotedAttributes() {
        val form = SamlFormParser.parse("<form action=/acs><input name=foo value=bar></form>")

        assertNotNull(form)
        assertEquals("/acs", form.action)
        assertEquals(HeadlessSsoMethod.POST, form.method)
        assertEquals("bar", form.fields["foo"])
    }
}
