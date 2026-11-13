# 06: Coda delle modifiche e tempistiche

**What to build:** la YouTube Sync diventa automatica. Quando mi iscrivo o disiscrivo in Flow la modifica parte subito (o appena torna la rete); le novità da YouTube arrivano all'apertura dell'app e ogni 6 ore in background; vedo lo stato e l'ora dell'ultima YouTube Sync.

**Blocked by:** 04 (Sincronizzazione continua delle iscrizioni)

**Status:** done (da verificare sul telefono, ticket 12)

- [x] Le modifiche locali alle iscrizioni entrano in una coda persistente che sopravvive a chiusura dell'app e riavvio
- [x] Modifiche consecutive sullo stesso elemento vengono compattate (iscrizione + disiscrizione si annullano)
- [x] Le modifiche arrivate tramite Device Sync da un altro dispositivo vengono trattate come modifiche locali e inviate a YouTube
- [x] Offline, la coda viene svuotata appena torna la rete
- [x] Lettura da YouTube all'apertura dell'app e con un lavoro in background ogni 6 ore che richiede la rete
- [x] Il lavoro in background gira solo sui profili con Linked Account
- [x] Indicatore discreto con stato e ora dell'ultima YouTube Sync riuscita
- [x] Una YouTube Sync in background con rimozioni di massa si sospende (`MassRemovalNotConfirmedException`) e mostra una notifica che apre l'anteprima (dal ticket 05)
- [x] La coda è generica, così che il ticket 09 possa accodarvi le operazioni sulle playlist

## Comments

- La coda è la differenza tra Flow e la Sync Baseline (ADR 0003), non un registro di operazioni. È persistente perché sono i dati stessi, si compatta da sola e include le modifiche arrivate con la Device Sync. Copre anche le playlist.
- Un osservatore (`YouTubeSyncStartup`) segue iscrizioni e tabelle delle playlist. Se Flow si è allontanato dalla Sync Baseline, avvia una YouTube Sync con WorkManager appena c'è rete.
- La lettura parte all'apertura dell'app (al massimo ogni 10 minuti) e con un lavoro periodico di 6 ore che richiede la rete. I lavori sono etichettati per profilo e si cancellano al cambio di profilo. Girano solo se c'è un Linked Account e solo dopo che l'anteprima del primo collegamento è stata confermata.
- L'indicatore di stato è la riga "Stato" nella pagina YouTube Sync. Mostra l'ora dell'ultima sincronizzazione riuscita, oppure il problema in corso.
- Una YouTube Sync in background con rimozioni di massa si sospende e manda una notifica. Toccandola si apre la pagina, che mostra subito l'anteprima.
- Test: `YouTubeSyncRunnerTest`.
