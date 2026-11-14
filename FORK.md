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

Il fork si installa **accanto** a Flow originale. La Device Sync resta compatibile: il telefono con il fork si accoppia e si sincronizza sia con una Firestick con Flow originale sia con una Firestick con il fork. L'account YouTube si collega solo dal telefono: su una TV il fork blocca il collegamento (ADR 0002).

### Dove vivono le modifiche al codice originale

Per tenere semplici i merge dal progetto originale, le modifiche sono isolate:

- `app/fork.gradle` contiene `applicationId`, la configurazione OAuth, la dipendenza AppAuth e le cartelle del fork. `app/build.gradle.kts` lo applica con **una sola riga** in fondo al file.
- `app/src/fork/java/` e `app/src/fork/res/` contengono il codice (package `io.github.aedev.flow.youtubesync`) e le stringhe della YouTube Sync. `app/src/forkTest/java/` contiene i suoi test. `app/src/forkFoss/res/` contiene il nome nel launcher.
- Le uniche righe toccate nei file originali, tutte marcate `// fork`, servono ad aggiungere la pagina "YouTube Sync" alle impostazioni. Sono in `SettingsDestination.kt`, `DestinationIndex.kt`, `SettingsDetail.kt` e `IntegrationsScreen.kt`.
- `Makefile` contiene i comandi di build, test e firma del fork.
- `.github/workflows/fork-build.yml` è la build del fork. I workflow originali (`build.yml`, `codeql.yml`, `labeler.yml`) **non vengono modificati**: sono disattivati dalle impostazioni Actions del fork.

## Build

### In locale

Il `Makefile` nella root raccoglie i comandi del fork; `make` da solo li elenca.

```bash
make release
```

La release si firma con `release.keystore` nella root e con `storePassword`, `keyAlias` e `keyPassword` in `local.properties` (entrambi esclusi da git). La prima volta crea la chiave con `make keystore`. Senza chiave, `make release` si ferma, perché l'APK uscirebbe **non firmato** e non si potrebbe installare.

Per provare senza chiave usa la build di debug, che ha `applicationId` con suffisso `.debug`. È molto più lenta della release (niente R8 e niente baseline profile): non usarla per giudicare le prestazioni, soprattutto sulla Firestick.

```bash
make debug
```

Sulla Firestick (32 bit) va installato l'APK `armeabi-v7a`.

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

## Collegare l'account YouTube (Google Cloud)

La YouTube Sync usa un progetto Google Cloud **tuo**, per uso personale. Le impostazioni di quel progetto si gestiscono solo dalla console di Google, quindi questi passi li fai tu, una volta sola.

1. Su [console.cloud.google.com](https://console.cloud.google.com) crea un progetto, per esempio "Flow YT".
2. In **API e servizi → Libreria** attiva **YouTube Data API v3**.
3. In **Google Auth Platform**:
   - **Branding**: inserisci nome dell'app ed email di supporto.
   - **Destinatari**: scegli il tipo **Esterno** e aggiungi il tuo account tra gli utenti di test.
   - **Accesso ai dati**: aggiungi l'ambito `https://www.googleapis.com/auth/youtube`.
4. Sempre in **Destinatari**, pubblica l'app **in produzione** senza richiedere la verifica. In modalità "Test" il login scade dopo 7 giorni. Al primo accesso Google mostrerà l'avviso "app non verificata": è previsto.
5. In **Client** crea un client OAuth di tipo **Android**:
   - Nome pacchetto: `io.github.fabrizioriccardo73.flow.debug` per la build di debug, oppure `io.github.fabrizioriccardo73.flow` per la release.
   - Impronta SHA-1 della chiave che firma l'APK. Per la build di debug:
     ```bash
     keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android | grep SHA1
     ```
   - In **Impostazioni avanzate** attiva **Schema URI personalizzato**. Serve perché AppAuth riceve la risposta su `<nome pacchetto>:/oauth2redirect`.
6. Copia l'ID client in `local.properties`, che non viene committato:
   ```properties
   youtubeSync.oauthClientId=1234567890-abc.apps.googleusercontent.com
   ```
7. Compila e installa la build di debug:
   ```bash
   ./gradlew :app:assembleFossDebug
   ```
8. In Flow YT apri Impostazioni → Integrazioni → **YouTube Sync** → **Collega account YouTube**.

**Se l'opzione "Schema URI personalizzato" non c'è** (Google la sta eliminando per i client Android), crea invece un client di tipo **iOS** con ID bundle `io.github.fabrizioriccardo73.flow`. Poi imposta in `local.properties` sia l'ID client sia lo schema iOS che Google mostra, cioè l'ID client rovesciato:

```properties
youtubeSync.oauthClientId=1234567890-abc.apps.googleusercontent.com
youtubeSync.oauthRedirectScheme=com.googleusercontent.apps.1234567890-abc
```

## Riallinearsi al progetto originale

Remote configurati:

- `origin`: `fabrizioriccardo73/Flow`, il fork
- `upstream`: `Flow-Tube/Flow`, il progetto originale

Per integrare gli aggiornamenti:

```bash
git fetch upstream && git merge upstream/main
```

I conflitti attesi sono rari. I punti di contatto con i file originali sono la riga `apply(from = "fork.gradle")` in fondo ad `app/build.gradle.kts`, una riga in cima a `README.md` e le righe marcate `// fork` nelle impostazioni. Dopo il merge, verifica che la build del fork resti verde.

## Trasferire i dati da Flow originale

1. In Flow originale, dalla sezione backup delle impostazioni, esporta il backup completo (ZIP).
2. In Flow YT, dalla stessa sezione, importa lo ZIP.
3. Controlla che iscrizioni, playlist e dati Flow-only ci siano tutti.
