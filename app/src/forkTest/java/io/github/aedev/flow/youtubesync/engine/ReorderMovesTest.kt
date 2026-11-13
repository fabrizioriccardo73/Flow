package io.github.aedev.flow.youtubesync.engine

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ReorderMovesTest {
    private fun applyMoves(
        current: List<String>,
        target: List<String>,
    ): Pair<List<String>, Int> {
        val list = current.toMutableList()
        val moves = reorderMoves(current, target)
        moves.forEach { (key, position) ->
            list.remove(key)
            list.add(position, key)
        }
        return list to moves.size
    }

    @Test
    fun `moving the first entry to the end is one move`() {
        val (result, moves) = applyMoves(listOf("a", "b", "c", "d"), listOf("b", "c", "d", "a"))
        assertThat(result).containsExactly("b", "c", "d", "a").inOrder()
        assertThat(moves).isEqualTo(1)
    }

    @Test
    fun `a reversed list needs all but one move`() {
        val (result, moves) = applyMoves(listOf("a", "b", "c", "d"), listOf("d", "c", "b", "a"))
        assertThat(result).containsExactly("d", "c", "b", "a").inOrder()
        assertThat(moves).isEqualTo(3)
    }

    @Test
    fun `entries outside the target stay where they are relative to their neighbours`() {
        val (result, _) = applyMoves(listOf("a", "x", "b", "c"), listOf("c", "a", "b"))
        assertThat(result.filter { it != "x" }).containsExactly("c", "a", "b").inOrder()
        assertThat(result).contains("x")
    }

    @Test
    fun `the same order needs no moves`() {
        assertThat(reorderMoves(listOf("a", "b"), listOf("a", "b"))).isEmpty()
    }

    @Test
    fun `merge - union without a baseline, YouTube order first`() {
        assertThat(mergeVideos(listOf("x", "a"), listOf("a", "b"), null)).containsExactly("a", "b", "x").inOrder()
    }
}
