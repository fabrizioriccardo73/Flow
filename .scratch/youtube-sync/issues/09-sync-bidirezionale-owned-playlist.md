# 09: Sincronizzazione bidirezionale delle Owned Playlist

**What to build:** le modifiche alle Owned Playlist fatte in Flow arrivano su YouTube e quelle fatte su YouTube arrivano in Flow: video aggiunti e rimossi, ordine, titolo, eliminazione della playlist, nuove playlist create su YouTube. Le Saved Playlist si aggiornano da YouTube.

**Blocked by:** 06 (Coda delle modifiche e tempistiche), 08 (Owned Playlist e Saved Playlist da YouTube a Flow)

**Status:** ready-for-agent

- [ ] Confronto a tre vie con la Sync Baseline per insieme delle playlist, video, ordine e titolo
- [ ] Un'eliminazione prevale su una modifica (es. rinominata in Flow, eliminata su YouTube → eliminata)
- [ ] Tra modifiche incompatibili vince YouTube (es. due ordinamenti diversi)
- [ ] Un video aggiunto da entrambe le parti compare una sola volta
- [ ] Le operazioni locali sulle Owned Playlist passano dalla coda del ticket 06
- [ ] La protezione dalle rimozioni di massa (ticket 05) vale anche per playlist e video
- [ ] Video eliminati o privati non vengono mai rimossi da YouTube
- [ ] Visibilità e descrizione delle playlist esistenti non vengono mai scritte
- [ ] Test di idempotenza e convergenza sulle playlist
