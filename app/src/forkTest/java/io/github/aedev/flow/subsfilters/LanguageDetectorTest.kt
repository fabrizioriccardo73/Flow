package io.github.aedev.flow.subsfilters

import com.google.common.truth.Truth.assertThat
import io.github.aedev.flow.subsfilters.language.LanguageDetector
import org.junit.Test

class LanguageDetectorTest {
    @Test
    fun `recognises clear Latin-script titles`() {
        assertThat(LanguageDetector.detect("Perché questo è il miglior telefono dell'anno? Ecco tutto quello che sono riuscito a provare")).isEqualTo("it")
        assertThat(LanguageDetector.detect("Why this is the best phone of the year and what you should know")).isEqualTo("en")
        assertThat(LanguageDetector.detect("Por qué este es el mejor teléfono del año y todo lo que hay que saber")).isEqualTo("es")
        assertThat(LanguageDetector.detect("Pourquoi c'est le meilleur téléphone de l'année avec tout ce que vous devez savoir")).isEqualTo("fr")
        assertThat(LanguageDetector.detect("Warum das das beste Handy des Jahres ist und was ich nicht mag")).isEqualTo("de")
        assertThat(LanguageDetector.detect("Por que esse é o melhor celular do ano e você não sabia disso")).isEqualTo("pt")
    }

    @Test
    fun `uses the start of the description when the title says little`() {
        assertThat(LanguageDetector.detect("iPhone 17 Pro", "In questo video vi faccio vedere tutto quello che c'è di nuovo, anche le cose che non mi sono piaciute"))
            .isEqualTo("it")
    }

    @Test
    fun `recognises non-Latin scripts`() {
        assertThat(LanguageDetector.detect("新しいスマホをレビューしてみた")).isEqualTo("ja")
        assertThat(LanguageDetector.detect("새로운 스마트폰 리뷰")).isEqualTo("ko")
        assertThat(LanguageDetector.detect("最新手机评测")).isEqualTo("zh")
        assertThat(LanguageDetector.detect("مراجعة الهاتف الجديد")).isEqualTo("ar")
    }

    @Test
    fun `stays unknown when the evidence is thin`() {
        assertThat(LanguageDetector.detect("iPhone 17 Pro Max")).isNull()
        assertThat(LanguageDetector.detect("🔥🔥🔥")).isNull()
        assertThat(LanguageDetector.detect("")).isNull()
        assertThat(LanguageDetector.detect("Minecraft 1.21 #shorts")).isNull()
    }
}
