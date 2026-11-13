# 05: Protezione dalle rimozioni di massa

**What to build:** se una YouTube Sync rimuoverebbe troppi elementi in una collezione (es. perché Flow ha perso i dati locali), Flow si ferma e mi chiede conferma invece di disiscrivermi da tutto su YouTube.

**Blocked by:** 04 (Sincronizzazione continua delle iscrizioni)

**Status:** ready-for-agent

- [x] La soglia scatta quando il piano rimuove più del 10% degli elementi di una collezione oppure più di 20 elementi, su uno qualsiasi dei due lati
- [x] Viene mostrato cosa verrebbe rimosso e da quale lato; posso confermare o rifiutare
- [x] Rifiutando non viene applicato nulla di quella YouTube Sync e la Sync Baseline resta invariata
- [ ] In background (senza interfaccia) la YouTube Sync si sospende e notifica che serve una conferma
- [x] La protezione è generica per collezione, così che valga anche per le playlist (ticket 09)
- [x] Test sul seam principale: scenario "dati locali svuotati con Sync Baseline presente" non tocca YouTube senza conferma

## Comments

**2026-11-13**: Implementato, non ancora provato sul telefono.
- `engine/RemovalGuard`: la soglia scatta con più di 20 rimozioni, oppure con più del 10% della collezione su quel lato **purché siano almeno 3**. Senza questo minimo, con 5 iscrizioni anche una sola disiscrizione (20%) chiederebbe conferma. È una precisazione rispetto alla spec; le costanti sono in `RemovalGuard`.
- `SyncPlan.massRemovals` indica collezione, lato, nomi e totale. `YouTubeSync.apply()` rifiuta un piano con rimozioni di massa lanciando `MassRemovalNotConfirmedException`, senza scrivere nulla, a meno di `massRemovalConfirmed = true`. Così nessun chiamante può aggirare la protezione, compresa la sincronizzazione in background del ticket 06.
- L'anteprima mostra l'avviso con lato, conteggio sul totale e i primi 8 nomi, e il pulsante "Rimuovi comunque". Annullando non si applica nulla e la Sync Baseline resta invariata.
- Resta aperto il criterio sulla sospensione in background con notifica: la sincronizzazione in background non esiste ancora. Va fatto nel ticket 06, che deve intercettare `MassRemovalNotConfirmedException`.
- Test: 6 in `MassRemovalTest` (dati persi in Flow, dati persi su YouTube, conferma, disiscrizione normale, nomi nell'avviso, soglie), tutti verdi insieme ai 34 precedenti.
