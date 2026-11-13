# 01: Fork e build

**What to build:** un fork personale di Flow per Android basato sulla variante FOSS, con un `applicationId` proprio, che compila con GitHub Actions sul fork. L'app si installa accanto a Flow originale, riceve i dati dall'app originale tramite il backup completo esistente e continua a fare Device Sync con la Firestick, che resta su Flow originale.

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

- [x] Il fork compila solo la variante FOSS con un `applicationId` proprio e un nome visibile distinguibile da Flow originale
- [ ] Una workflow GitHub Actions sul fork produce un APK firmato con una chiave del fork, scaricabile come artefatto
- [ ] Il fork si installa sul telefono accanto a Flow originale senza conflitti
- [ ] Il backup completo esportato da Flow originale si importa nel fork senza perdita di iscrizioni, playlist e dati Flow-only
- [ ] La Device Sync tra il fork (telefono) e Flow originale (Firestick) si accoppia e sincronizza come prima
- [x] Le note di copyright e la licenza GPL-3.0 originali sono conservate; il README del fork spiega cosa aggiunge e rimanda a `docs/adr/`
- [x] Documentato come riallineare il fork al progetto originale (remote upstream, branch del fork)

## Comments

**2026-11-13**: Fork creato (`fabrizioriccardo73/Flow`) e scaricato in questa cartella (`origin` = fork, `upstream` = Flow-Tube/Flow). `applicationId` `io.github.fabrizioriccardo73.flow` e nome "Flow YT" in `app/fork.gradle` + `app/src/fork/res`, applicati con una riga in fondo ad `app/build.gradle.kts`. Build locale `assembleFossRelease` verificata: l'APK riporta il nuovo package e l'etichetta. Workflow `.github/workflows/fork-build.yml` scritto ma non ancora eseguito: servono push, Actions attivate nel fork, workflow originali disattivati e i quattro secret di firma (vedi `FORK.md`). Restano da verificare a mano: installazione accanto a Flow originale, import del backup, Device Sync con la Firestick.

**2026-11-13**: Su richiesta, per ora si compila solo in locale. Tutti i workflow del fork, compreso `Fork build (FOSS)`, sono disattivati su GitHub e le esecuzioni partite con il primo push sono state annullate. Il criterio sulla build con GitHub Actions resta aperto finché `Fork build (FOSS)` non viene riattivato e i secret non vengono configurati.
