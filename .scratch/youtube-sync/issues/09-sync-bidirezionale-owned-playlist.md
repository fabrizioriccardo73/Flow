# 09: Sincronizzazione bidirezionale delle Owned Playlist

**What to build:** le modifiche alle Owned Playlist fatte in Flow arrivano su YouTube e quelle fatte su YouTube arrivano in Flow: video aggiunti e rimossi, ordine, titolo, eliminazione della playlist, nuove playlist create su YouTube. Le Saved Playlist si aggiornano da YouTube.

**Blocked by:** 06 (Coda delle modifiche e tempistiche), 08 (Owned Playlist e Saved Playlist da YouTube a Flow)

**Status:** done (da verificare sul telefono, ticket 12)

- [x] Confronto a tre vie con la Sync Baseline per insieme delle playlist, video, ordine e titolo
- [x] Un'eliminazione prevale su una modifica (es. rinominata in Flow, eliminata su YouTube → eliminata)
- [x] Tra modifiche incompatibili vince YouTube (es. due ordinamenti diversi)
- [x] Un video aggiunto da entrambe le parti compare una sola volta
- [x] Le operazioni locali sulle Owned Playlist passano dalla coda del ticket 06
- [x] La protezione dalle rimozioni di massa (ticket 05) vale anche per playlist e video
- [x] Video eliminati o privati non vengono mai rimossi da YouTube
- [x] Visibilità e descrizione delle playlist esistenti non vengono mai scritte
- [x] Test di idempotenza e convergenza sulle playlist

## Comments

- `PlaylistSync` fa il confronto a tre vie per playlist, titolo, video e ordine. Riordina su YouTube con il minor numero di spostamenti (sottosequenza crescente più lunga): ogni spostamento costa 50 unità di quota.
- Una playlist presente su entrambi i lati senza voce nella Sync Baseline viene unita (nell'ordine di YouTube, poi le aggiunte di Flow), senza rimuovere nulla.
- Se l'invio a YouTube di una playlist si interrompe, la sua voce nella Sync Baseline non cambia, così la YouTube Sync successiva la ripianifica.
- La rinomina rimanda la descrizione attuale, perché l'API sostituisce tutto lo snippet. Visibilità e descrizione non vengono mai cambiate.
- Test: `OwnedPlaylistsThreeWayTest` (16) e `ReorderMovesTest` (5).
