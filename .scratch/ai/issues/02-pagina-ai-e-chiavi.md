# 02: Pagina "AI" e Chiavi API per profilo

**What to build:** la pagina "AI" sotto Integrazioni dove si inseriscono e verificano le Chiavi API, si sceglie il Provider attivo e il modello; la memoria per profilo con le chiavi sigillate e lo stato "AI disponibile".

**Blocked by:** 01

**Status:** ready-for-agent

- [ ] Memoria per profilo (come `SubgroupLinks`): un record per provider (chiave sigillata con `KeystoreSecretBox`, HLC, tombstone) e un record per la scelta (Provider attivo, modello per provider, HLC); esclusa dai backup dell'app
- [ ] Flow "AI disponibile" per il profilo corrente: Provider attivo con Chiave API valida; un 401 in uso segna la chiave come non valida
- [ ] Pagina "AI" (righe `// fork: AI` in `SettingsDestination.kt`, `SettingsDetail.kt`, `IntegrationsScreen.kt`, come YouTube Sync)
- [ ] Per provider: campo chiave mascherato, link per ottenerla, verifica con `models` al salvataggio (non valida → non salvata, messaggio chiaro), cancellazione
- [ ] Scelta del Provider attivo tra quelli con chiave; scelta del modello dalla lista, con interruttore "mostra anche a pagamento" per OpenRouter
- [ ] Testo breve su cosa viene inviato e sull'uso dei prompt per l'addestramento nei tier gratuiti (ADR 0005)
- [ ] Sulla TV la pagina non permette di inserire chiavi (mostra solo lo stato)
- [ ] Stringhe in `src/fork/res` (it + en)
- [ ] Test JVM: sigillo e round trip, tombstone, disponibilità della AI, cancellazione della chiave del Provider attivo
