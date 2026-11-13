# 02: Sheet della Coda

**What to build:** dal dock del player apro uno sheet che mostra, nell'ordine in cui verranno riprodotti, cosa viene dopo dal Contesto e poi la Coda. Da lì riordino, rimuovo, svuoto e faccio partire i video.

**Blocked by:** 01 (Coda persistente e ordine Contesto → Coda → Autoplay)

**Status:** ready-for-agent

- [x] Lo sheet sostituisce `FlowPlaylistQueueBottomSheet` per i video
- [x] Sezione "Dopo, da: ‹Contesto›" in alto, con i controlli di oggi: riordino, rimozione, shuffle e loop *(decisione del 2026-11-13: ordine reale invece di "Coda in alto, Contesto in sola lettura")*
- [x] Sezione "Coda" sotto, con il video "In riproduzione" evidenziato
- [x] Trascinamento per riordinare la Coda, senza poter trascinare un video da una sezione all'altra
- [x] Swipe per rimuovere, con snackbar "Annulla"
- [x] "Svuota" rimuove tutta la Coda, con snackbar "Annulla"
- [x] Toccare un video della Coda lo fa partire e lo porta in cima, senza toglierlo dalla Coda
- [x] Il dock compare anche con un solo video in Coda e mostra il prossimo video nell'ordine reale
- [x] Stato vuoto chiaro quando la Coda è vuota
- [ ] Layout largo (pannello laterale) e stretto (dock flottante) entrambi funzionanti: da verificare sul telefono e sul tablet

## Comments

**2026-11-13**: Implementato, non ancora provato sul telefono.
- Lo sheet nuovo vive nel fork: `coda/ui/CodaQueueSheet.kt` e `CodaQueueSections.kt`. `PlayerQueueSheetHost` lo usa al posto di `FlowPlaylistQueueBottomSheet`, che resta nel codice originale inutilizzato per non complicare i merge.
- Il titolo dello sheet è "Next"/"A seguire". Ogni sezione ha la sua intestazione: il Contesto con la posizione, shuffle e loop; la Coda con il numero di video, "Svuota" e, senza riproduzione, "Riproduci".
- Il riordino usa `ReorderableLazyListState` una volta per sezione. Il componente condiviso ha un nuovo parametro `canMoveTo`, marcato `// fork`, che tiene il trascinamento dentro la propria sezione.
- Nuove operazioni sulla Coda: `move`, `remove` e `restore` (annulla), `clear` e `restoreAll`. Le annullano due nuove varianti di `QuickActionUndo`: `CodaRemoval` e `CodaCleared`.
- Non c'è una riga "Poi: Autoplay" in fondo.
