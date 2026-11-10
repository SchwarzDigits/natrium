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

package schwarz.digits.natrium.chat.protocol

import schwarz.digits.natrium.session.UpdateRecommended
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ProtocolUpdatesTest {

    @BeforeTest
    fun reset() = ProtocolUpdates.reset()

    @Test
    fun brokenContentRecommendsNoUpdate() {
        MessageProtocol.decode("""{"natrium":1,"type":"RESPONSE","response":{"type":"SINGLE_SELECT","selected":"yes"}}""")
        MessageProtocol.decode("""{"natrium":1,"type":"TEXT","text":"Hi","future":true}""")
        assertNull(ProtocolUpdates.updates.value)
    }

    @Test
    fun unknownTypeOfThisVersionRecommendsAnUpdate() {
        MessageProtocol.decode("""{"natrium":1,"type":"REQUEST","request":{"type":"RANKING","message":"x"}}""")
        assertEquals(UpdateRecommended(1, 1), ProtocolUpdates.updates.value)
    }

    @Test
    fun keepsTheHighestVersionSeen() {
        MessageProtocol.decode("""{"natrium":3,"type":"TEXT","text":"Hi"}""")
        MessageProtocol.decode("""{"natrium":2,"type":"TEXT","text":"Hi"}""")
        assertEquals(UpdateRecommended(1, 3), ProtocolUpdates.updates.value)
    }
}
