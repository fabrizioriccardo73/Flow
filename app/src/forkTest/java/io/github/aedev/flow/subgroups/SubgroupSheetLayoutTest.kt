package io.github.aedev.flow.subgroups

import com.google.common.truth.Truth.assertThat
import io.github.aedev.flow.data.model.SubscriptionGroup
import org.junit.Test

class SubgroupSheetLayoutTest {
    private val android = SubscriptionGroup("Android", listOf("UCa"))
    private val music = SubscriptionGroup("Music", listOf("UCm"))
    private val tech = SubscriptionGroup("Tech", listOf("UCt"))
    private val ai = SubscriptionGroup("AI", listOf("UCi"))

    @Test
    fun `each parent is followed by its Subgroups, indented`() {
        val layout = SubgroupSheetLayout(listOf(android, music, tech, ai), mapOf("Android" to "Tech", "AI" to "Tech"))
        assertThat(layout.groups).containsExactly(music, tech, android, ai).inOrder()
        assertThat(layout.groups.filter { layout.indented(it.name) }).containsExactly(android, ai)
    }

    @Test
    fun `without valid links the list stays as it is and nothing is indented`() {
        val layout = SubgroupSheetLayout(listOf(android, music), mapOf("Android" to "Gone"))
        assertThat(layout.groups).containsExactly(android, music).inOrder()
        assertThat(layout.indented("Android")).isFalse()
    }
}
