# 06: Sottogruppi nella Device Sync

**What to build:** i legami Sottogruppo → padre viaggiano tra i dispositivi tramite la collezione `fork_group_parents`; Flow originale li ignora e vede i Sottogruppi come Gruppi normali (ADR 0004).

**Blocked by:** 01, 02

**Status:** ready-for-agent

- [x] Collezione `fork_group_parents` registrata come `fork_channel_languages` (righe `// fork` in `Frames.kt`, `ChangeBatches.kt` con chiave il nome del Sottogruppo, `SyncApplier.kt`)
- [x] Merge per record, vince l'HLC più recente; tombstone rispettati
- [x] Una rinomina sincronizzata non lascia Sottogruppi sganciati sull'altro dispositivo
- [x] Test JVM: merge, convergenza, rinomina concorrente, peer che non dichiara la collezione
- [ ] Verifica manuale: telefono ↔ Firestick con fork e ↔ dispositivo con Flow originale

## Comments

**2026-11-14**: Implementato e compilato, non ancora provato tra dispositivi.
- `subgroups/SubgroupLinkRecords.kt`: `SubgroupLinkWire` (nome della collezione, chiave `group`, codifica canonica, merge per Sottogruppo con vittoria dell'HLC più recente, tombstone compresi).
- `subgroups/SubgroupLinks.kt`: `applyRemote` unisce i record arrivati e fa avanzare l'orologio HLC.
- `subsfilters/sync/ForkSync.kt` ora è generico: `COLLECTIONS`, `export(collection)`, `apply(collection, lines)` e `keyField(collection)`. Le righe `// fork` in `SyncApplier.kt` e `ChangeBatches.kt` passano per lui, così una nuova collezione del fork non tocca più i file originali. `Frames.kt` non cambia: prendeva già `ForkSync.COLLECTIONS`.
- Rinomina: quella sincronizzata porta con sé i legami riscritti, quindi sull'altro dispositivo i Sottogruppi restano sotto il padre rinominato. Se nel frattempo l'altro dispositivo ha messo un Gruppo dentro il vecchio nome, quel legame resta verso un Gruppo che non esiste più e il Gruppo compare come Gruppo normale: nessun dato perso, basta rimetterlo dentro.
- Test: `SubgroupLinkWireTest` (9: merge, tombstone, convergenza, round trip, rinomina, rinomina concorrente, peer che non dichiara la collezione, change batch) passa. Suite completa: falliscono solo `MusicBrainRankerTest` e `ProfileInitialTest`, come prima.
- Resta la verifica manuale telefono ↔ Firestick e con Flow originale.
