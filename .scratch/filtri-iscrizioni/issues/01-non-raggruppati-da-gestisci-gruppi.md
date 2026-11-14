# 01: Attivare "Non raggruppati" da Gestisci gruppi

**What to build:** in "Gestisci gruppi" un interruttore mostra o nasconde il chip "Non raggruppati", senza passare dalle impostazioni.

**Blocked by:** nessuno

**Status:** ready-for-agent

- [x] Interruttore in "Gestisci gruppi" (telefono), accanto alla riga "Non raggruppati"
- [x] Scrive la stessa preferenza di Impostazioni → Contenuti → "Gruppo Non raggruppati"; i due interruttori restano allineati
- [x] "Tutti" non cambia comportamento
- [x] Codice in `src/fork`, agganci `// fork` minimi nei file originali

## Comments

**2026-11-14**: Implementato, non ancora provato sul telefono.
- `subsfilters/ui/UngroupedChipToggle.kt` nel fork: un `FlowSwitchRow` con un piccolo ViewModel che legge e scrive `PlayerPreferences.subscriptionUngroupedEnabled`, la stessa preferenza di Impostazioni → Contenuti.
- In `SubscriptionGroupDialogs.kt` (originale) la lista dei gruppi e l'interruttore stanno in una `Column`; tre righe marcate `// fork: Filtri iscrizioni` più l'import. L'interruttore compare solo quando c'è almeno un gruppo, perché senza gruppi il chip non comparirebbe comunque.
- Flow originale non traduce in italiano "Ungrouped": aggiunte in `src/fork/res/values-it` le traduzioni di `group_ungrouped` e delle due stringhe dell'impostazione. Probabile motivo per cui il chip non si riconosceva.
