# 05: Sottogruppi nel foglio dei gruppi del canale

**What to build:** quando assegno un canale ai Gruppi, i Sottogruppi compaiono rientrati sotto il loro padre, ognuno con la sua casella.

**Blocked by:** 01

**Status:** ready-for-agent

- [x] Lista nel foglio ordinata come l'albero: padre, poi i suoi Sottogruppi rientrati
- [x] Spuntare un Sottogruppo non spunta il padre
- [x] Aggancio `// fork` minimo in `ChannelGroupSheet.kt`

## Comments

**2026-11-14**: Implementato e compilato, non ancora provato sul telefono.
- `subgroups/SubgroupSheet.kt`: `SubgroupSheetLayout`, l'ordine dell'albero (`treeOrder`) e quali righe vanno rientrate.
- `subgroups/ui/SubgroupSheet.kt`: `rememberSubgroupSheetLayout` legge i legami da `SubgroupLinks.observe()` e `rowModifier` dà il rientro.
- Originale: `ChannelGroupSheet.kt` (lista nell'ordine dell'albero e rientro) e `SaveToCollectionSheet.kt` (parametro opzionale `entryModifier`, accanto a `header` dei Filtri iscrizioni), righe `// fork: Sottogruppi`.
- Ogni riga resta legata al proprio Gruppo, quindi spuntare un Sottogruppo non tocca il padre.
- Test: `SubgroupSheetLayoutTest` (2) passa.
