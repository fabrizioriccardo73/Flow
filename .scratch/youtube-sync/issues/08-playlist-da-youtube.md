# 08: Owned Playlist e Saved Playlist da YouTube a Flow

**What to build:** al primo collegamento (e alle YouTube Sync successive per le nuove playlist) le mie Owned Playlist e le Saved Playlist arrivano in Flow con il loro ID YouTube e il loro tipo. Le Saved Playlist sono in sola lettura. Le playlist Flow-only restano tali, e l'anteprima mi avvisa se una ha lo stesso titolo di una Owned Playlist.

**Blocked by:** 03 (Primo collegamento delle iscrizioni)

**Status:** ready-for-agent

- [ ] `YouTubeAccount` espone l'elenco delle playlist distinguendo Owned Playlist e Saved Playlist, e l'elenco paginato dei video di una playlist; adattatore reale corrispondente
- [ ] Ogni playlist importata conserva in Flow il proprio ID YouTube e il tipo (Owned/Saved)
- [ ] Le playlist arrivate da YouTube viaggiano tra i dispositivi con la Device Sync come le altre
- [ ] Le Saved Playlist sono in sola lettura nell'interfaccia di Flow
- [ ] Video eliminati o privati vengono ignorati e non compaiono in Flow
- [ ] Le playlist Flow-only non vengono inviate a YouTube
- [ ] L'anteprima del primo collegamento include le playlist e avvisa dei titoli uguali senza unirle
- [ ] Test sul seam principale per import, tipi, sola lettura ed elementi non rappresentabili
