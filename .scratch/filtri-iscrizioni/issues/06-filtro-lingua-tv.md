# 06: Filtro lingua sulla TV

**What to build:** in `TvSubscriptionsScreen` c'è lo stesso Filtro lingua del telefono, navigabile con il telecomando.

**Blocked by:** 04, 05

**Status:** ready-for-agent

- [x] Comando del filtro nella sezione Iscrizioni TV, con scelta multipla
- [x] Stesse regole del telefono (somma al gruppo, sconosciuta passa sempre, Device-local)
- [x] Sulla TV non si assegna la Lingua del canale
- [ ] Verifica manuale sulla Firestick

## Comments

**2026-11-14**: Implementato, non ancora provato sulla Firestick.
- La TV usa gli stessi `SubscriptionsFeedContent` e `SubscriptionsFeedHeader` del telefono, quindi il chip compare da solo. `TvSubscriptionsScreen` usa `rememberLanguageFiltered` (righe `// fork: Filtri iscrizioni`).
- La riga "Lingua" del foglio dei gruppi è nascosta sulla TV (`ThisDevice.isTelevision`).
