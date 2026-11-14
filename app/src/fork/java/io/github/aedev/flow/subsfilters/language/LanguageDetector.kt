package io.github.aedev.flow.subsfilters.language

/**
 * Guesses a video's language from its title and description, on the device and with no library.
 *
 * Non-Latin scripts decide directly (Hangul, kana, Han, Arabic, Devanagari). Latin text is scored by
 * frequent words that belong to one language only, plus a few letters typical of one language. The
 * guess must be clear: when the evidence is thin or two languages are close, the result is null, and
 * the Language filter always shows a video it cannot place.
 */
object LanguageDetector {
    /** Only the start of a description counts: the rest is links, sponsors and hashtags. */
    private const val DESCRIPTION_CHARS = 400
    private const val MIN_SCORE = 2
    private const val MIN_SCRIPT_SHARE = 0.3

    private val STOPWORDS: Map<String, Set<String>> =
        mapOf(
            "it" to
                setOf(
                    "gli", "della", "delle", "degli", "dello", "nella", "nelle", "nello", "sono", "che", "questo", "questa",
                    "questi", "anche", "più", "perché", "cosa", "tutto", "tutti", "molto", "ecco", "ancora", "dove",
                    "alla", "alle", "sulla", "sul", "dei", "nel", "ho", "abbiamo", "fatto", "fare", "nuovo", "nuova",
                    "oggi", "sempre", "mai", "senza", "dopo", "prima", "ci", "vi",
                ),
            "en" to
                setOf(
                    "the", "and", "of", "to", "is", "you", "this", "with", "for", "how", "what", "my", "your", "are",
                    "why", "best", "new", "from", "that", "have", "will", "can", "just", "about", "it's", "we",
                    "they", "their", "be", "been", "was", "were", "which", "when", "everything", "should", "would",
                ),
            "es" to
                setOf(
                    "el", "los", "las", "por", "y", "muy", "pero", "cómo", "qué", "más", "este", "esta", "esto", "hoy",
                    "nuevo", "mejor", "todo", "todos", "hacer", "cuando", "también", "años", "ya", "hay", "nunca",
                    "ahora", "así", "puedes", "tengo", "mis", "sus",
                ),
            "fr" to
                setOf(
                    "les", "des", "du", "est", "une", "et", "pour", "pas", "avec", "dans", "cette", "mon",
                    "vous", "nous", "au", "aux", "très", "comment", "pourquoi", "être", "sont", "mes",
                    "tout", "faire", "c'est", "j'ai", "ça", "leur", "aussi",
                ),
            "de" to
                setOf(
                    "der", "die", "und", "ist", "nicht", "ein", "eine", "mit", "auf", "für", "ich", "wir", "zu", "den",
                    "dem", "im", "von", "auch", "oder", "mein", "neue", "heute", "sich", "bei", "noch",
                    "alles", "warum", "wird", "einen",
                ),
            "pt" to
                setOf(
                    "uma", "não", "são", "você", "com", "dos", "na", "em", "muito", "isso", "esse",
                    "essa", "também", "ao", "meu", "minha", "hoje", "novo", "melhor", "vocês", "nós", "fazer", "ele",
                    "ela", "está", "pra", "foi",
                ),
            "nl" to
                setOf(
                    "het", "een", "van", "niet", "voor", "ik", "wat", "hoe", "mijn", "dit", "deze", "naar",
                    "ook", "nieuwe", "zijn", "wij", "jullie", "maar", "nog", "waarom", "wordt",
                ),
        )

    private val LETTER_HINTS: List<Pair<Regex, String>> =
        listOf(
            Regex("[ñ¿¡]") to "es",
            Regex("[ãõ]") to "pt",
            Regex("[ß]") to "de",
            Regex("[äöü]") to "de",
            Regex("[ìòù]") to "it",
            Regex("[êâîôûëïœ]") to "fr",
            Regex("ij") to "nl",
        )

    private val WORD = Regex("[\\p{L}']+")

    fun detect(
        title: String,
        description: String = "",
    ): String? {
        val text = (title + "\n" + description.take(DESCRIPTION_CHARS)).trim()
        if (text.isEmpty()) return null
        byScript(text)?.let { return it }
        return byWords(text.lowercase())
    }

    private fun byScript(text: String): String? {
        var letters = 0
        val counts = HashMap<String, Int>()
        for (ch in text) {
            if (!ch.isLetter()) continue
            letters++
            val script = Character.UnicodeScript.of(ch.code)
            val language =
                when (script) {
                    Character.UnicodeScript.HANGUL -> "ko"
                    Character.UnicodeScript.HIRAGANA, Character.UnicodeScript.KATAKANA -> "ja"
                    Character.UnicodeScript.HAN -> "han"
                    Character.UnicodeScript.ARABIC -> "ar"
                    Character.UnicodeScript.DEVANAGARI -> "hi"
                    else -> null
                } ?: continue
            counts.merge(language, 1, Int::plus)
        }
        if (letters == 0) return null
        val nonLatin = counts.values.sum()
        if (nonLatin < letters * MIN_SCRIPT_SHARE) return null
        return when {
            // Japanese writes kanji beside kana; Chinese has no kana.
            (counts["ja"] ?: 0) > 0 -> "ja"
            (counts["ko"] ?: 0) > 0 -> "ko"
            else -> counts.maxByOrNull { it.value }?.key?.let { if (it == "han") "zh" else it }
        }
    }

    private fun byWords(text: String): String? {
        val scores = HashMap<String, Int>()
        for (match in WORD.findAll(text)) {
            val word = match.value.trim('\'')
            for ((language, words) in STOPWORDS) {
                if (word in words) scores.merge(language, 1, Int::plus)
            }
        }
        for ((pattern, language) in LETTER_HINTS) {
            if (pattern.containsMatchIn(text)) scores.merge(language, 1, Int::plus)
        }
        val ranked = scores.entries.sortedByDescending { it.value }
        val best = ranked.firstOrNull() ?: return null
        val second = ranked.getOrNull(1)?.value ?: 0
        // Clear means enough evidence and at least twice the runner-up.
        return best.key.takeIf { best.value >= MIN_SCORE && best.value >= second * 2 }
    }
}
