# 03: Chiavi API nella Device Sync

**What to build:** Chiavi API, Provider attivo e modello viaggiano tra i dispositivi nella collezione `fork_ai_keys` (ADR 0006); Flow originale la ignora.

**Blocked by:** 02

**Status:** ready-for-agent

- [ ] Collezione `fork_ai_keys` registrata tramite `ForkSync`, senza nuove righe nei file originali
- [ ] Chiave del record: il provider (più un record fisso per la scelta)
- [ ] Sul wire la chiave in chiaro dentro il pacchetto cifrato; all'arrivo sigillata con `KeystoreSecretBox`
- [ ] Merge per record, vince l'HLC più recente; tombstone rispettati (una chiave cancellata sparisce ovunque)
- [ ] Test JVM: merge, convergenza, tombstone, round trip, peer che non dichiara la collezione
- [ ] Verifica manuale: telefono → Firestick con fork (la AI diventa disponibile sulla TV) e telefono ↔ dispositivo con Flow originale
