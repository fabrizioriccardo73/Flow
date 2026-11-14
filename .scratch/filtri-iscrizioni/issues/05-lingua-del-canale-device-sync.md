# 05: Lingua del canale nella Device Sync

**What to build:** le Lingue del canale viaggiano tra i dispositivi con il fork tramite la collezione `fork_channel_languages`; i dispositivi con Flow originale non la ricevono (ADR 0004).

**Blocked by:** 02

**Status:** ready-for-agent

- [x] Collezione `fork_channel_languages` dichiarata `produce`/`consume` solo dal fork
- [x] Merge per voce, vince l'HLC più recente; tombstone rispettati
- [x] Nessun campo aggiunto a collezioni esistenti
- [x] Test JVM sul merge e su un peer che non dichiara la collezione
- [ ] Verifica manuale: telefono (fork) ↔ Firestick con fork e ↔ Firestick con Flow originale

## Comments

**2026-11-14**: Implementato, non ancora provato tra dispositivi.
- `subsfilters/sync/ChannelLanguageRecords.kt`: record, codifica canonica, merge per canale (vince l'HLC più alto, tombstone compresi).
- `subsfilters/sync/ForkSync.kt`: esportazione e applicazione, raggiunte dal codice originale tramite un entry point Hilt, così i costruttori originali non cambiano.
- Originale: `Frames.kt` (`ANDROID_SYNCABLE` + collezioni del fork), `ChangeBatches.kt` (chiave `channelId`), `SyncApplier.kt` (esporta e applica).
- Correzione all'ADR: la Device Sync (sync2) non negozia le collezioni. Flow originale riceve i pacchetti e li ignora, e prima fa il suo backup di sicurezza. La negoziazione esiste solo nella vecchia FLOW-SYNC/1, che non offre la collezione.
- Test: `ChannelLanguageRecordsTest` (merge, convergenza, wire). Resta la verifica manuale con la Firestick.
