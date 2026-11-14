# 01: Albero dei Sottogruppi e scope del padre

**What to build:** il legame Sottogruppo → padre salvato per profilo, l'albero ricavato dai legami validi e lo scope dei chip: il padre include i canali dei suoi Sottogruppi.

**Blocked by:** nessuno

**Status:** ready-for-agent

- [x] Memoria per profilo nel fork: nome del Sottogruppo → nome del padre, con HLC e tombstone; nessun campo aggiunto alle entità o ai record canonici dei Gruppi
- [x] Funzione pura `(Gruppi, legami) → albero`: un legame vale solo se il Sottogruppo e il padre esistono e il padre non è a sua volta un Sottogruppo; i legami non validi si ignorano senza riscrivere dati
- [x] Scope del padre = canali propri ∪ canali dei Sottogruppi validi, senza doppioni; scope del Sottogruppo = solo i suoi canali
- [x] Aggancio `// fork` dove oggi si calcola `groupSelectionScope`; "Non raggruppati" invariato
- [x] Test JVM: legami non validi, due livelli dopo un merge, padre mancante, scope, Non raggruppati, round trip della persistenza

## Comments

**2026-11-14**: Implementato, non ancora provato sul telefono. Senza il ticket 02 non c'è ancora un modo per creare un legame dall'app.
- `subgroups/SubgroupTree.kt`: albero dai legami validi (un legame vale se i due Gruppi esistono, sono diversi e il padre non ha a sua volta un legame verso un Gruppo esistente; copre i due livelli dopo un merge, i cicli e il padre eliminato) e `scopeWithSubgroups`.
- `subgroups/SubgroupLinkRecords.kt`: record con HLC e tombstone, codifica per la memoria. Il merge e la codifica sul wire arrivano con il ticket 06.
- `subgroups/SubgroupLinks.kt`: memoria per profilo (`fork_group_parents`, `PROFILE_LIBRARY`), `setParent(group, parent?)` e `observe()` per il codice originale, raggiunto da un entry point Hilt tramite `FlowApplication.appContext`, così il costruttore del ViewModel non cambia.
- Originale: `SubscriptionsUiState.kt` (campo `subgroupLinks` e `selectedGroupScope` che passa per `scopeWithSubgroups`) e `SubscriptionsViewModel.kt` (raccolta dei legami), righe marcate `// fork: Sottogruppi`. Il telefono e la TV usano lo stesso ViewModel.
- Test: `SubgroupTreeTest` (7) e `SubgroupLinkRecordsTest` (3) passano, come `SubscriptionGroupSelectionTest` e `SubscriptionsViewModelTest`. Nella suite completa falliscono due test che non riguardano i gruppi: `MusicBrainRankerTest` (ordine che dipende dall'ora) e `ProfileInitialTest` (emoji di bandiera).
