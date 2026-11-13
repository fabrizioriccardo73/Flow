# Flow YT: fork personale di Flow

Questo repository è un fork personale di [Flow-Tube/Flow](https://github.com/Flow-Tube/Flow) che aggiunge la **YouTube Sync**: la sincronizzazione bidirezionale di iscrizioni e playlist tra un profilo Flow e un account YouTube, tramite l'API ufficiale di YouTube con OAuth.

- Il progetto originale esclude il login Google. Questa funzione esiste solo qui e non verrà proposta al progetto originale.
- Uso personale, nessuna distribuzione pubblica.
- La licenza resta GPL-3.0, come il progetto originale (vedi `License`). Il copyright originale appartiene ad A-EDev.

## Documentazione

- `GLOSSARY.md`: il vocabolario del dominio (YouTube Sync, Device Sync, Linked Account, Sync Baseline…)
- `docs/adr/`: le decisioni architetturali del fork
- `.scratch/youtube-sync/spec.md`: la spec
- `.scratch/youtube-sync/issues/`: i ticket

## Cosa cambia rispetto al progetto originale

| | Flow originale | Questo fork |
|---|---|---|
| `applicationId` | `io.github.aedev.flow` | `io.github.fabrizioriccardo73.flow` |
| Nome nel launcher | Flow | Flow YT |
| Varianti | `github`, `foss` | solo `foss` |
| Firma | chiave ufficiale di Flow | chiave del fork |

Il fork si installa **accanto** a Flow originale. La Device Sync resta compatibile: il telefono con il fork e la Firestick con Flow originale continuano ad accoppiarsi e a sincronizzarsi.

### Dove vivono le modifiche al codice originale

Per tenere semplici i merge dal progetto originale, le modifiche sono isolate:

- `app/fork.gradle` contiene `applicationId` e le risorse del fork. `app/build.gradle.kts` lo applica con **una sola riga** in fondo al file.
- `app/src/fork/res/` contiene le risorse del fork, per esempio il nome nel launcher.
- `.github/workflows/fork-build.yml` è la build del fork. I workflow originali (`build.yml`, `codeql.yml`, `labeler.yml`) **non vengono modificati**: sono disattivati dalle impostazioni Actions del fork.
- Il codice della YouTube Sync vivrà in un'area separata (vedi `docs/adr/0002-…`).

## Build

### In locale

```bash
./gradlew :app:assembleFossRelease
```

Senza `release.keystore` nella root, l'APK di release esce **non firmato**. Per provare sul telefono senza chiave, usa la build di debug, che ha `applicationId` con suffisso `.debug`:

```bash
./gradlew :app:assembleFossDebug
```

### Con GitHub Actions

> **Al momento tutti i workflow del fork sono disattivati** (Actions → seleziona il workflow → "Enable workflow" per riattivarlo): per ora si compila solo in locale. Quando si riattiva `Fork build (FOSS)`, lasciare disattivati i workflow originali.

Il workflow `Fork build (FOSS)` parte a ogni push su `main`, oppure a mano da Actions → "Run workflow". Esegue i test unitari FOSS, compila l'APK di release firmato con la chiave del fork e lo carica come artefatto.

Servono quattro secret nel repository del fork (Settings → Secrets and variables → Actions):

| Secret | Contenuto |
|---|---|
| `FORK_KEYSTORE_BASE64` | il keystore del fork, codificato in base64 |
| `FORK_STORE_PASSWORD` | la password del keystore |
| `FORK_KEY_ALIAS` | l'alias della chiave |
| `FORK_KEY_PASSWORD` | la password della chiave |

Per creare la chiave, una volta sola: conserva il keystore e le password in un posto sicuro, per esempio nel password manager. Se perdi la chiave, devi disinstallare e reinstallare l'app. In più l'impronta SHA-1 della chiave va registrata nel client OAuth di Google (ticket 02), quindi cambiarla significa aggiornare anche quella.

```bash
keytool -genkeypair -v -keystore flow-yt-release.keystore -alias flow-yt -keyalg RSA -keysize 4096 -validity 10000
```

```bash
base64 -i flow-yt-release.keystore | gh secret set FORK_KEYSTORE_BASE64 --repo fabrizioriccardo73/Flow
```

```bash
gh secret set FORK_KEY_ALIAS --repo fabrizioriccardo73/Flow --body flow-yt
```

```bash
gh secret set FORK_STORE_PASSWORD --repo fabrizioriccardo73/Flow
```

```bash
gh secret set FORK_KEY_PASSWORD --repo fabrizioriccardo73/Flow
```

Gli ultimi due comandi chiedono la password in modo interattivo, così non finisce nella cronologia della shell.

## Riallinearsi al progetto originale

Remote configurati:

- `origin`: `fabrizioriccardo73/Flow`, il fork
- `upstream`: `Flow-Tube/Flow`, il progetto originale

Per integrare gli aggiornamenti:

```bash
git fetch upstream && git merge upstream/main
```

I conflitti attesi sono rari. L'unico punto di contatto con i file originali è la riga `apply(from = "fork.gradle")` in fondo ad `app/build.gradle.kts`, più una riga in cima a `README.md`. Dopo il merge, verifica che la build del fork resti verde.

## Trasferire i dati da Flow originale

1. In Flow originale, dalla sezione backup delle impostazioni, esporta il backup completo (ZIP).
2. In Flow YT, dalla stessa sezione, importa lo ZIP.
3. Controlla che iscrizioni, playlist e dati Flow-only ci siano tutti.
