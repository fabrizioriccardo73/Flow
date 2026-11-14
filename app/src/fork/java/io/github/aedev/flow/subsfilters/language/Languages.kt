package io.github.aedev.flow.subsfilters.language

import java.util.Locale

/** The languages a channel can be given, as ISO 639-1 codes; the detector recognises all but Russian. */
object Languages {
    val ASSIGNABLE: List<String> = listOf("it", "en", "es", "fr", "de", "pt", "nl", "ru", "ja", "ko", "zh", "ar", "hi")

    /** The language's name in the device's language, capitalised: "Italiano", "Inglese". */
    fun displayName(
        code: String,
        locale: Locale = Locale.getDefault(),
    ): String =
        Locale
            .forLanguageTag(code)
            .getDisplayLanguage(locale)
            .ifBlank { code }
            .replaceFirstChar { it.titlecase(locale) }

    /** The short label of a set of languages, in [ASSIGNABLE] order: "IT+EN". */
    fun shortLabel(codes: Set<String>): String = sorted(codes).joinToString("+") { it.uppercase(Locale.ROOT) }

    /** [codes] in [ASSIGNABLE] order, unknown codes last. */
    fun sorted(codes: Collection<String>): List<String> =
        codes.sortedWith(compareBy<String> { ASSIGNABLE.indexOf(it).let { i -> if (i < 0) Int.MAX_VALUE else i } }.thenBy { it })
}
