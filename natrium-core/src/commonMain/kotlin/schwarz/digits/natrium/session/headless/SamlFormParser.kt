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

/** A parsed HTML `<form>` — the SAML POST-binding auto-submit form a browser would submit. */
internal data class SamlForm(
    val action: String,
    val method: HeadlessSsoMethod,
    val fields: Map<String, String>,
)

/**
 * Minimal, dependency-free extractor for the first `<form>` of an HTML document and its
 * `<input>` fields. This is what a browser's JavaScript auto-submit does for the SAML HTTP-POST
 * binding — no full HTML engine is needed to reproduce it.
 */
internal object SamlFormParser {

    private val FORM_RE = Regex(
        "<form\\b([^>]*)>(.*?)</form>",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    private val INPUT_RE = Regex(
        "<input\\b([^>]*?)/?>",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )

    /** Returns the first form, or `null` if the document contains no `<form>`. */
    fun parse(html: String): SamlForm? {
        val form = FORM_RE.find(html) ?: return null
        val attrs = form.groupValues[1]
        val body = form.groupValues[2]

        val action = attribute(attrs, "action")?.let(::htmlUnescape) ?: return null
        val method = if (attribute(attrs, "method")?.equals("get", ignoreCase = true) == true) {
            HeadlessSsoMethod.GET
        } else {
            HeadlessSsoMethod.POST
        }

        val fields = LinkedHashMap<String, String>()
        for (match in INPUT_RE.findAll(body)) {
            val inputAttrs = match.groupValues[1]
            val name = attribute(inputAttrs, "name")?.let(::htmlUnescape) ?: continue
            val value = attribute(inputAttrs, "value")?.let(::htmlUnescape) ?: ""
            fields[name] = value
        }

        return SamlForm(action = action, method = method, fields = fields)
    }

    /** Reads a single HTML attribute value (double-quoted, single-quoted, or unquoted). */
    private fun attribute(tag: String, name: String): String? {
        val re = Regex(
            "\\b" + Regex.escape(name) + "\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s>]+))",
            RegexOption.IGNORE_CASE,
        )
        val match = re.find(tag) ?: return null
        val (dq, sq, uq) = match.destructured
        return when {
            match.groups[1] != null -> dq
            match.groups[2] != null -> sq
            else -> uq
        }
    }

    private fun htmlUnescape(text: String): String {
        if ('&' !in text) return text
        // &amp; must be resolved LAST so that e.g. "&amp;lt;" becomes "&lt;", not "<".
        return text
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&#x2F;", "/")
            .replace("&#47;", "/")
            .replace("&amp;", "&")
    }
}
