# 10: Collegare una playlist Flow-only

**What to build:** da una playlist creata in Flow posso scegliere "Collega a YouTube": viene creata una nuova playlist privata sul mio account con gli stessi video e lo stesso ordine, e da lì in poi è una Owned Playlist sincronizzata.

**Blocked by:** 09 (Sincronizzazione bidirezionale delle Owned Playlist)

**Status:** ready-for-agent

- [ ] L'azione "Collega a YouTube" compare solo sulle playlist Flow-only di un profilo con Linked Account
- [ ] Viene sempre creata una nuova playlist privata su YouTube, mai unita a una esistente per titolo
- [ ] Titolo, video e ordine vengono copiati; i video non rappresentabili non esistono in Flow quindi non serve gestirli
- [ ] L'ID YouTube viene salvato e la playlist diventa Owned Playlist, anche sugli altri dispositivi via Device Sync
- [ ] Se la creazione fallisce a metà, la YouTube Sync successiva completa senza creare una seconda playlist
- [ ] Test sul seam principale
