# 🎬 YouTube Account Migrator (`yt-migrator`)

CLI interattiva ed evoluta per trasferire e sincronizzare **canali seguiti (iscrizioni)** e **playlist personalizzate** da un account YouTube (Sorgente A) ad un altro (Destinazione B).

---

## ✨ Caratteristiche principali

* ⚡ **Nessuna configurazione Google Cloud Console**: Basato sul motore interno Innertube (`youtubei.js`), non richiede di creare progetti GCP, né chiavi API o configurazioni OAuth complesse sul cloud.
* 🚀 **Zero limiti di quota giornaliera**: Supera il limite stringente delle API ufficiali (che limiterebbero il trasferimento a sole 200 iscrizioni al giorno).
* 🔐 **Login semplificato (Device Code Flow)**: Mostra un link (`google.com/device`) e un codice alfanumerico: fai login comodamente nel browser e confermi. Le credenziali vengono memorizzate e rinfrescate in locale (`~/.yt-migrator`).
* 🧙‍♂️ **Wizard interattivo e moderno**: Interfaccia da terminale pulita e guidata con `@clack/prompts`, spinner reattivi, barre di avanzamento e menu di selezione.
* 🛡️ **Rilevamento duplicati e Idempotenza**: Salta automaticamente i canali già seguiti sull'account B; rileva conflitti di playlist permettendo di unire i video mancanti o creare duplicati.
* ⏱️ **Protezione Anti-Spam (Jitter & Rate Limiting)**: Ritardo casuale controllato (500–1500ms) tra le richieste per evitare blocchi o limitazioni da parte di YouTube.
* 💾 **Salvataggio dello stato & Resume**: Se l'operazione viene interrotta (Ctrl+C, caduta di connessione, ecc.), la migrazione può essere ripresa da dove si era fermata con `--resume`.
* 🧪 **Modalità Anteprima (Dry-run)**: Flag `--dry-run` per simulare l'intero processo e visualizzare il riepilogo senza modificare gli account.

---

## 🚀 Avvio Rapido

### Prerequisiti
* Node.js v18 o superiore

### Installazione ed Esecuzione

Dalla cartella principale del progetto:

```bash
cd tools/yt-migrator

# Avvio diretto in modalità sviluppo:
npm run dev

# Oppure compilando ed eseguendo:
npm run build
npm start
```

Puoi anche renderlo accessibile globalmente nel tuo sistema tramite link npm:
```bash
npm link
yt-migrator
```

---

## 🛠️ Opzioni e Flag da riga di comando

```bash
yt-migrator [opzioni]
```

| Opzione | Descrizione |
| :--- | :--- |
| `-d, --dry-run` | Simula l'intera procedura a schermo **senza effettuare modifiche** su YouTube |
| `-r, --resume` | Riprende automaticamente l'ultima migrazione incompiuta salvata nel checkpoint |
| `--only-subscriptions` | Trasferisce **solo i canali seguiti**, ignorando le playlist |
| `--only-playlists` | Trasferisce **solo le playlist personalizzate**, ignorando le iscrizioni |
| `--delay <ms>` | Ritardo base in millisecondi tra le singole operazioni (default: `1000` ms) |
| `--reset-auth` | Elimina le sessioni memorizzate e richiede un nuovo accesso per entrambi gli account |
| `--data-dir <path>` | Specifica una cartella personalizzata per salvare token e checkpoint (default: `~/.yt-migrator`) |
| `-h, --help` | Mostra la guida dei comandi |

### Esempi:

```bash
# Esegui una simulazione sicura senza toccare nulla:
npm run dev -- --dry-run

# Migra solo i canali seguiti:
npm run dev -- --only-subscriptions

# Resetta le credenziali salvate e rifai il login:
npm run dev -- --reset-auth
```

---

## 🧭 Flusso del Wizard Guidato

1. **Autenticazione**:
   * Ti verrà mostrato l'indirizzo `https://www.google.com/device` e un codice univoco per l'**Account A** (da cui copiare).
   * Ripeti la procedura per l'**Account B** (su cui copiare).
   * La CLI mostrerà il nome/email associato a ciascun account per evitare inversioni accidentali.
2. **Scansione**:
   * La CLI legge la lista delle iscrizioni e delle playlist di entrambi gli account.
3. **Selezione**:
   * Scegli se copiare tutti i canali o selezionare solo quelli desiderati.
   * Scegli la strategia per le playlist duplicate (es. unire i video mancanti o creare nuova playlist).
4. **Riepilogo & Conferma**:
   * Mostra il numero esatto di canali da iscrivere, quanti sono già presenti (e quindi saltati), e il tempo stimato.
5. **Esecuzione & Report**:
   * Mostra l'avanzamento in tempo reale con status per ogni canale e playlist.
   * Stampa un report finale chiaro con il totale dei trasferimenti completati.
