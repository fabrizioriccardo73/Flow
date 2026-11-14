# 04: Seconda fila di chip sulla TV

**What to build:** lo stesso comportamento del ticket 03 nella sezione Iscrizioni della TV, navigabile con il D-pad.

**Blocked by:** 03

**Status:** ready-for-agent

- [x] Seconda fila in `TvSubscriptionsScreen` raggiungibile con il D-pad dalla fila principale
- [x] Stesse regole di selezione, scope e persistenza del telefono
- [ ] Verifica manuale sulla Firestick

## Comments

**2026-11-14**: Implementato e compilato, non ancora provato sulla Firestick.
- La TV usa già `SubscriptionsFeedHeader` (via `SubscriptionsFeedContent`), quindi la seconda fila, le regole di selezione, lo scope e la persistenza sono quelli del ticket 03, senza righe nuove in `TvSubscriptionsScreen`.
- `subgroups/ui/SubgroupChipRow.kt`: la fila è un gruppo di focus con `focusRestorer`. Scendendo con il D-pad dalla fila principale si arriva sull'ultimo chip della fila che aveva il focus, altrimenti sul Sottogruppo selezionato, altrimenti sul primo; non su quello che capita sotto il chip a fuoco. Sul telefono non cambia nulla.
- Resta la verifica manuale sulla Firestick.
