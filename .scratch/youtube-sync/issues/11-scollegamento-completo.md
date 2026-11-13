# 11: Scollegamento completo

**What to build:** quando scollego il Linked Account non perdo nulla in Flow: iscrizioni e playlist restano, le Owned Playlist e le Saved Playlist diventano playlist Flow-only, e su YouTube non viene toccato niente.

**Blocked by:** 09 (Sincronizzazione bidirezionale delle Owned Playlist)

**Status:** done (da verificare sul telefono, ticket 12)

- [x] Scollegando, Owned Playlist e Saved Playlist diventano Flow-only (modificabili, senza legame a YouTube)
- [x] Sync Baseline e coda vengono eliminate senza inviare nulla a YouTube
- [x] Il permesso viene revocato e i token eliminati (estende il ticket 02)
- [x] Ricollegando lo stesso o un altro account si riparte come al primo collegamento, con anteprima
- [x] Test sul seam principale: nessuna chiamata di scrittura verso `YouTubeAccount` durante lo scollegamento

## Comments

- Allo scollegamento si revocano permesso e token (ticket 02). Poi le Owned Playlist, e le Saved Playlist arrivate da YouTube, diventano Flow-only con un nuovo ID. La Sync Baseline e lo stato vengono cancellati, i lavori in background fermati, e non si fa nessuna chiamata a YouTube.
- Test: `LinkAndReleasePlaylistsTest` e `YouTubeSyncRunnerTest`.
