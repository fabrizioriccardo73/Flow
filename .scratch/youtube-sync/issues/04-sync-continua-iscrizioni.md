# 04: Sincronizzazione continua delle iscrizioni

**What to build:** dopo il primo collegamento, ogni "Sincronizza ora" porta da una parte all'altra le iscrizioni e disiscrizioni fatte da un lato solo dall'ultima YouTube Sync, confrontando Flow, YouTube e la Sync Baseline.

**Blocked by:** 03 (Primo collegamento delle iscrizioni)

**Status:** ready-for-agent

- [ ] Un'iscrizione aggiunta in Flow viene creata su YouTube, e viceversa
- [ ] Una disiscrizione in Flow viene applicata su YouTube, e viceversa
- [ ] Un'iscrizione aggiunta su entrambi i lati risulta presente una sola volta
- [ ] La Sync Baseline viene aggiornata solo dopo l'applicazione riuscita; per gli elementi falliti resta invariata così che si ritentino
- [ ] Il motore produce un piano completo prima di qualsiasi scrittura (presupposto per i ticket 05 e 09)
- [ ] Test di idempotenza: due YouTube Sync consecutive senza modifiche non producono operazioni
- [ ] Test di convergenza: dopo una YouTube Sync riuscita le iscrizioni coincidono
- [ ] I gruppi di iscrizioni restano Flow-only e non vengono toccati
