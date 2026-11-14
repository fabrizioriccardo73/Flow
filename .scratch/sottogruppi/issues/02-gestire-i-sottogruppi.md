# 02: Gestire i Sottogruppi da "Gestisci gruppi"

**What to build:** in "Gestisci gruppi" (telefono e TV, stesso dialogo) si sceglie il padre di un Gruppo, si vedono i Sottogruppi rientrati e si riordinano; rinomina ed eliminazione rispettano i legami.

**Blocked by:** 01

**Status:** ready-for-agent

- [x] Campo "Dentro a" nel dialogo di creazione/modifica: "nessuno" più i Gruppi che non sono Sottogruppi (escluso il Gruppo stesso)
- [x] Un Gruppo con Sottogruppi ha il campo disattivato, con una riga che spiega perché
- [x] Lista dei Gruppi con i Sottogruppi rientrati sotto il padre; il riordino dentro un padre riscrive il `sortOrder`
- [x] Rinomina: tombstone sul record del vecchio nome, nuovo record, padre riscritto nei record dei Sottogruppi
- [x] Eliminazione di un padre: tombstone sui legami dei Sottogruppi, che restano Gruppi; il dialogo di conferma lo dice
- [x] Stringhe in `src/fork/res` (it + en)
- [x] Test JVM su rinomina (padre e Sottogruppo) ed eliminazione del padre

## Comments

**2026-11-14**: Implementato e compilato, non ancora provato sul telefono né sulla TV.
- `subgroups/SubgroupLinkRecords.kt`: `afterSave` (nuovo Gruppo, cambio di padre, rinomina del Sottogruppo o del padre) e `afterDelete` (chiude il legame del Gruppo e quelli dei suoi Sottogruppi). Si scrive solo ciò che cambia.
- `subgroups/SubgroupTree.kt`: `parentOptions` (solo Gruppi che non puntano a un padre, mai quello in modifica), `hasSubgroups`, `treeOrder` (ogni padre seguito dai suoi Sottogruppi; un Sottogruppo trascinato fuori torna sotto il padre) e `treeMove` (i pulsanti su/giù del D-pad: un Sottogruppo si sposta tra i fratelli, un padre si sposta con i suoi Sottogruppi).
- `subgroups/ui/SubgroupGroupDialogs.kt`: riga "Dentro a" con la scelta del padre, rientro dei Sottogruppi, conferma di eliminazione solo per un Gruppo che ha Sottogruppi (oggi Flow elimina i Gruppi senza chiedere, e per gli altri resta così), `SubgroupEditsViewModel` che salva i legami.
- Originale: `SubscriptionGroupDialogs.kt`, righe `// fork: Sottogruppi`. Due parametri opzionali (`arrangement`, `extraContent`) e gli agganci nell'host. La TV usa lo stesso host, quindi ha tutto.
- L'ordine salvato è sempre quello dell'albero, così su Flow originale ogni Sottogruppo compare subito dopo il suo padre.
- Test: `SubgroupEditsTest` (11) passa.
