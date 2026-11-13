# 11: Scollegamento completo

**What to build:** quando scollego il Linked Account non perdo nulla in Flow: iscrizioni e playlist restano, le Owned Playlist e le Saved Playlist diventano playlist Flow-only, e su YouTube non viene toccato niente.

**Blocked by:** 09 (Sincronizzazione bidirezionale delle Owned Playlist)

**Status:** ready-for-agent

- [ ] Scollegando, Owned Playlist e Saved Playlist diventano Flow-only (modificabili, senza legame a YouTube)
- [ ] Sync Baseline e coda vengono eliminate senza inviare nulla a YouTube
- [ ] Il permesso viene revocato e i token eliminati (estende il ticket 02)
- [ ] Ricollegando lo stesso o un altro account si riparte come al primo collegamento, con anteprima
- [ ] Test sul seam principale: nessuna chiamata di scrittura verso `YouTubeAccount` durante lo scollegamento
