# 08: Owned Playlist e Saved Playlist da YouTube a Flow

**What to build:** al primo collegamento (e alle YouTube Sync successive per le nuove playlist) le mie Owned Playlist e le Saved Playlist arrivano in Flow con il loro ID YouTube e il loro tipo. Le Saved Playlist sono in sola lettura. Le playlist Flow-only restano tali, e l'anteprima mi avvisa se una ha lo stesso titolo di una Owned Playlist.

**Blocked by:** 03 (Primo collegamento delle iscrizioni)

**Status:** done (da verificare sul telefono, ticket 12)

- [x] `YouTubeAccount` espone l'elenco delle playlist distinguendo Owned Playlist e Saved Playlist, e l'elenco paginato dei video di una playlist; adattatore reale corrispondente
- [x] Ogni playlist importata conserva in Flow il proprio ID YouTube e il tipo (Owned/Saved)
- [x] Le playlist arrivate da YouTube viaggiano tra i dispositivi con la Device Sync come le altre
- [x] Le Saved Playlist sono in sola lettura nell'interfaccia di Flow
- [x] Video eliminati o privati vengono ignorati e non compaiono in Flow
- [x] Le playlist Flow-only non vengono inviate a YouTube
- [x] L'anteprima del primo collegamento include le playlist e avvisa dei titoli uguali senza unirle
- [x] Test sul seam principale per import, tipi, sola lettura ed elementi non rappresentabili

## Comments

- Limite dell'API ufficiale: la YouTube Data API non permette di leggere le Saved Playlist (le playlist di altri salvate in raccolta; issue Google 79957181). Con il Canale ufficiale arrivano quindi solo le Owned Playlist. Il motore, il modello e l'interfaccia in sola lettura gestiscono già le Saved Playlist (verificato con il finto account). Arriveranno quando ci sarà il Canale cookie. La pagina YouTube Sync lo spiega con una riga informativa.
- Una playlist importata usa l'ID YouTube come proprio ID (ADR 0003). Le Saved Playlist sono "playlist salvate" di Flow, già in sola lettura nell'interfaccia.
- Test: `PlaylistsFromYouTubeTest`.
