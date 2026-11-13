# 06: Coda delle modifiche e tempistiche

**What to build:** la YouTube Sync diventa automatica. Quando mi iscrivo o disiscrivo in Flow la modifica parte subito (o appena torna la rete); le novità da YouTube arrivano all'apertura dell'app e ogni 6 ore in background; vedo lo stato e l'ora dell'ultima YouTube Sync.

**Blocked by:** 04 (Sincronizzazione continua delle iscrizioni)

**Status:** ready-for-agent

- [ ] Le modifiche locali alle iscrizioni entrano in una coda persistente che sopravvive a chiusura dell'app e riavvio
- [ ] Modifiche consecutive sullo stesso elemento vengono compattate (iscrizione + disiscrizione si annullano)
- [ ] Le modifiche arrivate tramite Device Sync da un altro dispositivo vengono trattate come modifiche locali e inviate a YouTube
- [ ] Offline, la coda viene svuotata appena torna la rete
- [ ] Lettura da YouTube all'apertura dell'app e con un lavoro in background ogni 6 ore che richiede la rete
- [ ] Il lavoro in background gira solo sui profili con Linked Account
- [ ] Indicatore discreto con stato e ora dell'ultima YouTube Sync riuscita
- [ ] Una YouTube Sync in background con rimozioni di massa si sospende (`MassRemovalNotConfirmedException`) e mostra una notifica che apre l'anteprima (dal ticket 05)
- [ ] La coda è generica, così che il ticket 09 possa accodarvi le operazioni sulle playlist
