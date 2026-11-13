# 10: Collegare una playlist Flow-only

**What to build:** da una playlist creata in Flow posso scegliere "Collega a YouTube": viene creata una nuova playlist privata sul mio account con gli stessi video e lo stesso ordine, e da lì in poi è una Owned Playlist sincronizzata.

**Blocked by:** 09 (Sincronizzazione bidirezionale delle Owned Playlist)

**Status:** done (da verificare sul telefono, ticket 12)

- [x] L'azione "Collega a YouTube" compare solo sulle playlist Flow-only di un profilo con Linked Account
- [x] Viene sempre creata una nuova playlist privata su YouTube, mai unita a una esistente per titolo
- [x] Titolo, video e ordine vengono copiati; i video non rappresentabili non esistono in Flow quindi non serve gestirli
- [x] L'ID YouTube viene salvato e la playlist diventa Owned Playlist, anche sugli altri dispositivi via Device Sync
- [x] Se la creazione fallisce a metà, la YouTube Sync successiva completa senza creare una seconda playlist
- [x] Test sul seam principale

## Comments

- L'azione è nella pagina YouTube Sync: "Collega una playlist a YouTube" apre l'elenco delle playlist Flow-only. Si trova lì invece che nella pagina della playlist per non toccare l'interfaccia originale di Flow. Compare solo con un Linked Account.
- Il collegamento crea la playlist privata e dà alla playlist di Flow l'ID YouTube. I video partono con la YouTube Sync successiva, avviata subito. Se si interrompe, la sincronizzazione dopo completa i video senza creare una seconda playlist.
- Test: `LinkAndReleasePlaylistsTest`.
