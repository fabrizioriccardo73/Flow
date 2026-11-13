# 02: Sheet della Coda

**What to build:** dal dock del player apro uno sheet che mostra la Coda e, sotto, cosa viene dopo dal Contesto. Da lì riordino, rimuovo, svuoto e faccio partire i video.

**Blocked by:** 01 (Coda persistente e ordine Contesto → Coda → Autoplay)

**Status:** ready-for-agent

- [ ] Lo sheet sostituisce `FlowPlaylistQueueBottomSheet` per i video
- [ ] Sezione "Coda" in alto, con il video "In riproduzione" etichettato in cima
- [ ] Sezione "Dopo, da: ‹Contesto›" sotto, in sola lettura, con i prossimi video della playlist o dell'Autoplay, nell'ordine reale di riproduzione
- [ ] Trascinamento per riordinare la Coda
- [ ] Swipe per rimuovere, con snackbar "Annulla"
- [ ] "Svuota" rimuove tutta la Coda, con snackbar "Annulla"
- [ ] Toccare un video della Coda lo sposta in cima e lo fa partire, senza toglierlo dalla Coda
- [ ] Il dock compare anche con un solo video in Coda e mostra il prossimo video nell'ordine reale
- [ ] Stato vuoto chiaro quando la Coda è vuota
- [ ] Layout largo (pannello laterale) e stretto (dock flottante) entrambi funzionanti
