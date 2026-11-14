# 03: Seconda fila di chip sul telefono

**What to build:** nella sezione Iscrizioni la fila principale mostra solo i Gruppi senza padre; selezionando un padre compare la fila dei suoi Sottogruppi per restringere il filtro.

**Blocked by:** 01

**Status:** ready-for-agent

- [x] Fila principale: "Tutti", Gruppi senza padre, "Non raggruppati" (se attivo)
- [x] Seconda fila sotto la principale solo quando il padre selezionato ha Sottogruppi validi
- [x] Nessun Sottogruppo selezionato = tutto il padre; tocco = restringe; secondo tocco = torna al padre; un solo Sottogruppo alla volta
- [x] Filtro lingua e "Accesso rapido" seguono la selezione più stretta
- [x] Il Sottogruppo selezionato viene ricordato insieme al Gruppo selezionato; se non è più valido, si torna a quel Gruppo come Gruppo normale, se esiste

## Comments

**2026-11-14**: Implementato e compilato, non ancora provato sul telefono.
- La selezione salvata resta un solo nome, il chip più stretto: il Sottogruppo se ce n'è uno selezionato, altrimenti il Gruppo. Il padre si ricava dal legame. Così il codice originale (scope del feed, Post, Filtro lingua, Accesso rapido, persistenza e rinomina) segue il Sottogruppo senza nuove chiavi. Un Sottogruppo il cui legame non vale più resta selezionato come Gruppo normale nella fila principale; se è stato eliminato si torna a "Tutti", come per ogni Gruppo.
- Eliminando da "Gestisci gruppi" il Sottogruppo selezionato si passa al suo padre invece che a "Tutti".
- `subgroups/SubgroupChips.kt`: fila principale senza Sottogruppi, chip acceso in ciascuna fila, seconda fila, tocco/secondo tocco, `selectionAfterDelete`.
- `subgroups/ui/SubgroupChipRow.kt`: la seconda fila, sotto la principale, solo se il padre selezionato ha Sottogruppi validi.
- Originale: `SubscriptionsFeedHeader.kt` (fila principale e seconda fila) e `SubscriptionGroupDialogs.kt` (selezione dopo l'eliminazione), righe `// fork: Sottogruppi`.
- La TV usa lo stesso `SubscriptionsFeedHeader`, quindi la seconda fila compare già anche lì: per il ticket 04 resta da verificare la navigazione con il D-pad sulla Firestick.
- Test: `SubgroupChipsTest` (7) passa, come gli altri test dei Sottogruppi, `SubscriptionGroupSelectionTest` e `SubscriptionsViewModelTest`.
