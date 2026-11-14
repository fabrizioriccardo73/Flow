# 04: Filtro lingua sul telefono

**What to build:** un chip con il globo in testa alla fila dei chip della sezione Iscrizioni permette di mostrare solo i video di una o più lingue.

**Blocked by:** 02, 03

**Status:** ready-for-agent

- [x] Chip fisso in testa alla fila; mostra le lingue attive ("IT", "IT+EN") o nessuna
- [x] Scelta multipla tra le lingue presenti nelle iscrizioni
- [x] Si somma al chip del gruppo selezionato
- [x] Vale per video, Shorts, dirette e programmati; non tocca "Accesso rapido"
- [x] I video con Lingua del video sconosciuta passano sempre
- [x] Selezione salvata per profilo (`PROFILE_PRIVATE`), non nella Device Sync
- [x] Test JVM sul predicato combinato con lo scope del gruppo

## Comments

**2026-11-14**: Implementato, non ancora provato sul telefono.
- `subsfilters/filter/LanguageFilter.kt` (singleton): selezione nel DataStore `fork_language_filter` (`PROFILE_PRIVATE`), lingue disponibili, filtro.
- `subsfilters/ui/LanguageFilterUi.kt`: `rememberLanguageFiltered(uiState)` restringe video e Shorts; `LanguageFilterChip` con il globo apre l'elenco con interruttori, "Applica" e "Mostra tutto".
- Originale: `SubscriptionsFeedHeader` mostra il chip in testa alla fila, `SubscriptionsScreen` passa lo stato filtrato al feed (righe `// fork: Filtri iscrizioni`).
- Limite: il chip compare anche nella vista Post, che non viene filtrata.
