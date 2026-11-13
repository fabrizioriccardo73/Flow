# 04: Sincronizzazione continua delle iscrizioni

**What to build:** dopo il primo collegamento, ogni "Sincronizza ora" porta da una parte all'altra le iscrizioni e disiscrizioni fatte da un lato solo dall'ultima YouTube Sync, confrontando Flow, YouTube e la Sync Baseline.

**Blocked by:** 03 (Primo collegamento delle iscrizioni)

**Status:** ready-for-agent

- [x] Un'iscrizione aggiunta in Flow viene creata su YouTube, e viceversa
- [x] Una disiscrizione in Flow viene applicata su YouTube, e viceversa
- [x] Un'iscrizione aggiunta su entrambi i lati risulta presente una sola volta
- [x] La Sync Baseline viene aggiornata solo dopo l'applicazione riuscita; per gli elementi falliti resta invariata così che si ritentino
- [x] Il motore produce un piano completo prima di qualsiasi scrittura (presupposto per i ticket 05 e 09)
- [x] Test di idempotenza: due YouTube Sync consecutive senza modifiche non producono operazioni
- [x] Test di convergenza: dopo una YouTube Sync riuscita le iscrizioni coincidono
- [x] I gruppi di iscrizioni restano Flow-only e non vengono toccati

## Comments

**2026-11-13**: Implementato, non ancora provato sul telefono.
- `YouTubeSync.plan()` ora confronta Flow, YouTube e la Sync Baseline. Un canale presente da un solo lato è stato aggiunto lì se non è nella Sync Baseline, altrimenti è stato rimosso dall'altro lato: la modifica viene portata dall'altra parte. Un canale aggiunto su entrambi i lati, o rimosso da entrambi, non genera operazioni.
- `apply()` scrive prima in Flow, poi su YouTube le aggiunte e infine le rimozioni. La nuova Sync Baseline contiene i canali su cui i due lati concordano, più le rimozioni non ancora riuscite su YouTube. Così un'aggiunta fallita resta fuori e una rimozione fallita resta dentro, ed entrambe vengono ritentate.
- Le rimozioni in Flow passano da `SubscriptionRepository.unsubscribe`, come una disiscrizione manuale: lasciano la traccia che la Device Sync propaga agli altri dispositivi.
- I gruppi di iscrizioni non vengono letti né scritti. Una disiscrizione arrivata da YouTube ha sui gruppi lo stesso effetto di una disiscrizione manuale in Flow.
- L'anteprima mostra aggiunte e rimozioni per lato e va ancora confermata a ogni "Sincronizza ora". La protezione automatica dalle rimozioni di massa arriva con il ticket 05.
- Test: 10 nuovi in `ThreeWaySubscriptionsTest` (propagazione nei due versi, aggiunte e rimozioni da entrambi i lati, idempotenza, convergenza, nuovi tentativi), tutti verdi insieme ai 24 precedenti.
