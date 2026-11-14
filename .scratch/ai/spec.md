# Spec: Funzioni AI

Status: ready-for-agent

Vedi anche: `GLOSSARY.md` (Provider AI, Chiave API, Provider attivo, Riassunto video, Domande sul video, Riassunto commenti, Organizzazione assistita, Proposta, Flow-only, Device-local), ADR 0004, ADR 0005, ADR 0006.

## Problem Statement

Flow non ha nessuna funzione AI. Per capire se un video lungo vale la pena devo guardarlo o scorrere la trascrizione a mano; per sapere cosa ne pensa il pubblico devo leggere centinaia di commenti; per mettere in ordine molte iscrizioni devo assegnare uno per uno Gruppi e Lingua del canale. Non voglio pagare un servizio né rinunciare alla promessa di privacy di Flow: voglio usare i tier gratuiti di un Provider AI con la mia Chiave API, e decidere io ogni volta cosa parte.

## Solution

- **Provider AI**: Google AI Studio, Groq e OpenRouter. L'utente inserisce una Chiave API per ciascuno che vuole usare e sceglie il Provider attivo e il modello. Uno solo alla volta, nessun passaggio automatico.
- **AI disponibile**: un profilo ha funzioni AI se e solo se ha un Provider attivo con una Chiave API valida. Altrimenti tutti i punti d'ingresso sono nascosti; resta solo la pagina "AI" nelle Impostazioni.
- **Privacy** (ADR 0005): ogni richiesta parte da un'azione esplicita; mai cronologia o dati FlowNeuro; niente in background. Eccezione dichiarata: l'Organizzazione assistita invia i canali scelti dall'utente e i nomi dei suoi Gruppi, dopo una schermata che mostra cosa parte.
- **Riassunto video**: punti chiave con timestamp cliccabili, dalla trascrizione, nella lingua dell'app. Senza trascrizione non è disponibile.
- **Domande sul video**: conversazione su un video, risposte dalla trascrizione con timestamp cliccabili. Non viene conservata. Solo telefono.
- **Riassunto commenti**: dai ~200 commenti top, senza nomi degli autori; sezioni fisse.
- **Organizzazione assistita**: Proposte di Gruppo/Sottogruppo esistente, nuovo Gruppo e Lingua del canale per i canali scelti; nulla si applica senza conferma.
- **Device Sync** (ADR 0006): Chiavi API, Provider attivo e modello viaggiano nella collezione `fork_ai_keys`. Le chiavi si inseriscono solo dal telefono.
- **TV**: Riassunto video e Riassunto commenti in sola lettura.

## User Stories

### Configurazione

1. Come utente, voglio una pagina "AI" sotto Integrazioni, accanto a YouTube Sync.
2. Come utente, voglio inserire una Chiave API per Google AI Studio, Groq e OpenRouter, ognuna con un link alla pagina del provider dove ottenerla.
3. Come utente, voglio che una chiave venga verificata al salvataggio e, se non vale, non venga salvata e mi venga detto perché (chiave non valida o rete assente).
4. Come utente, voglio scegliere il Provider attivo tra quelli con una chiave salvata.
5. Come utente, voglio un modello predefinito per ogni provider e poterlo cambiare da una lista scaricata dal provider.
6. Come utente di OpenRouter, voglio vedere di default solo i modelli gratuiti, con un interruttore per vederli tutti.
7. Come utente, voglio cancellare una chiave; se era quella del Provider attivo, la AI risulta non disponibile.
8. Come utente, voglio che la pagina mi dica in breve cosa viene inviato al provider e che i tier gratuiti possono usare i prompt per l'addestramento.
9. Come utente con più profili, voglio che chiavi, Provider attivo e modello siano del profilo.
10. Come utente con telefono e Firestick, voglio che le chiavi e la scelta del provider inserite sul telefono arrivino sulla TV.
11. Come utente che non usa la AI, voglio che Flow sia identico all'originale: nessun pulsante AI da nessuna parte.

### Riassunto video e Domande sul video

12. Come utente, voglio un'azione "Riassunto" nel foglio della descrizione del video.
13. Come utente, voglio un riassunto con i punti chiave e il momento in cui compaiono; toccando un momento il player ci salta.
14. Come utente, voglio il riassunto nella lingua dell'app anche se il video è in un'altra lingua.
15. Come utente, voglio che un video senza trascrizione mostri l'azione disattivata con il motivo.
16. Come utente, voglio un riassunto completo anche per video di ore, anche con provider dai limiti stretti.
17. Come utente, voglio ritrovare un riassunto già fatto senza consumare quota, e poterlo rigenerare.
18. Come utente, voglio fare domande sul video e ricevere risposte con i momenti citati, cliccabili.
19. Come utente, voglio sapere quando una risposta si basa solo su una parte della trascrizione.

### Riassunto commenti

20. Come utente, voglio un pulsante "Riassunto" in cima al foglio dei commenti.
21. Come utente, voglio un riassunto con opinione generale, punti ricorrenti, critiche e correzioni o aggiunte segnalate.
22. Come utente, voglio ritrovarlo senza consumare quota, e poterlo rigenerare.

### TV

23. Come utente sulla TV, voglio leggere il Riassunto video e il Riassunto commenti navigando col telecomando, con i momenti del Riassunto video selezionabili.

### Organizzazione assistita

24. Come utente, voglio avviare l'Organizzazione assistita dalla gestione dei Gruppi della sezione Iscrizioni.
25. Come utente, voglio scegliere quali canali analizzare; di default quelli Non raggruppati o senza Lingua del canale.
26. Come utente, voglio vedere prima dell'invio cosa parte: quanti canali, quali dati per canale, i nomi dei miei Gruppi.
27. Come utente, voglio una lista di Proposte per canale (Gruppo o Sottogruppo esistente, nuovo Gruppo, Lingua del canale) con accetta/rifiuta e "accetta tutte".
28. Come utente, voglio che accettare una Proposta di nuovo Gruppo crei il Gruppo e ci metta il canale.
29. Come utente con molte iscrizioni, voglio che l'analisi proceda a lotti e che un errore su un lotto non faccia perdere le Proposte degli altri.

### Errori

30. Come utente, voglio un messaggio chiaro quando il provider risponde con un limite di quota (429), con il suggerimento di riprovare o cambiare provider.
31. Come utente, voglio un messaggio chiaro quando la chiave non vale più.

## Implementation Decisions

- Il codice vive in `app/src/fork/java/io/github/aedev/flow/ai/` (sottopacchetti per client, chiavi, riassunti, organizzazione, ui). Stringhe in `app/src/fork/res` (it + en). Test in `app/src/forkTest`.
- **Righe `// fork: AI`** nei file originali: una per punto d'ingresso, che chiama un composable del fork. Punti: `SettingsDestination.kt`, `SettingsDetail.kt` e `IntegrationsScreen.kt` (pagina "AI", come YouTube Sync); il foglio della descrizione del player (`PlayerDescriptionSheetHost`, usato anche dalla TV in `TvPlayerSheets.kt`); `FlowCommentsBottomSheet.kt`; `SubscriptionGroupDialogs.kt` (gestione dei Gruppi). Elencarle in `FORK.md`.
- **Client**: un unico client HTTP compatibile OpenAI (OkHttp e kotlinx.serialization, già nel progetto), nessun SDK. Per provider cambiano base URL e header:
  - Google AI Studio: `https://generativelanguage.googleapis.com/v1beta/openai/`
  - Groq: `https://api.groq.com/openai/v1/`
  - OpenRouter: `https://openrouter.ai/api/v1/` (header facoltativi `HTTP-Referer` / `X-Title` con un nome generico dell'app)
  - Endpoint usati: `GET models` (verifica della chiave e lista dei modelli) e `POST chat/completions`. Errori mappati in un tipo del fork: chiave non valida (401/403), limite (429, con `Retry-After` se presente), contesto troppo lungo, rete, altro.
- **Modelli predefiniti**: uno per provider, scelto all'implementazione tra i modelli gratuiti con il contesto più ampio, in una costante del fork aggiornabile con un rilascio. Il filtro "solo gratuiti" di OpenRouter usa il suffisso `:free` dell'id. Per ogni modello serve una stima del budget di contesto: dalla risposta di `models` se il provider la dà, altrimenti un valore prudente.
- **Memoria delle chiavi**: per profilo, come `SubgroupLinks` (scope del profilo). Un record per provider (chiave, HLC, tombstone) e un record per la scelta (Provider attivo, modello per provider, HLC). La chiave è salvata sigillata con `KeystoreSecretBox`, mai in chiaro nelle preferenze. Esclusa dai backup dell'app.
- **AI disponibile**: un flow osservabile `(profilo) → disponibile/non disponibile`, letto da tutti i punti d'ingresso; la verifica avviene solo al salvataggio, poi un 401 in uso marca la chiave come non valida finché l'utente non la reinserisce.
- **Device Sync**: collezione `fork_ai_keys`, registrata tramite `ForkSync` (`COLLECTIONS`, `export`, `apply`, `keyField`), senza nuove righe nei file originali. Sul wire la chiave è in chiaro dentro il pacchetto, che la Device Sync cifra; all'arrivo viene sigillata. Merge per record, vince l'HLC più recente, tombstone rispettati. La TV non mostra mai il campo per inserire una chiave.
- **Prompt**: in costanti del fork, con la lingua dell'app passata esplicitamente. Le risposte strutturate (Riassunto video, Riassunto commenti, Proposte) si chiedono in JSON e si validano; una risposta che non si interpreta è un errore "risposta non valida" con "Riprova".
- **Trascrizione**: si usa `TranscriptRepository` con la traccia che il player già sceglie (anche automatica). I timestamp viaggiano nel prompt come `[mm:ss]` e tornano come millisecondi validati contro la durata del video.
- **Divisione in parti**: se la trascrizione supera il budget del modello, si divide in parti consecutive per tempo, si riassume ogni parte e poi si uniscono i riassunti. Le richieste delle parti sono in sequenza, così da rispettare i limiti per minuto.
- **Domande sul video**: se la trascrizione entra nel budget, il contesto è la trascrizione; altrimenti i riassunti delle parti più le parti più vicine alla domanda (scelte dall'AI stessa sui riassunti delle parti), e la risposta porta il segnale "contesto parziale". La conversazione vive solo nel ViewModel.
- **Commenti**: i primi ~200 commenti nell'ordine "top" tramite `CommentsPager`, solo testo e numero di like, senza risposte né autori.
- **Cache**: Device-local e per profilo, per (video, tipo di riassunto, lingua dell'app). Contiene anche provider e modello che l'hanno prodotta, mostrati sotto il riassunto. "Rigenera" la sostituisce. Nessun limite di età; un limite di numero (per esempio le ultime 200 voci) evita che cresca senza fine.
- **Organizzazione assistita**: per canale si inviano nome, descrizione e ultimi ~5 titoli, insieme all'albero dei Gruppi esistenti (nomi e legami di Sottogruppo). Lotti di dimensione fissa (per esempio 20 canali). Le Proposte valide: Gruppo o Sottogruppo esistente (per nome esatto), nuovo Gruppo (mai un nuovo Sottogruppo), Lingua del canale tra le lingue che il fork supporta. Proposte che citano Gruppi inesistenti si scartano. Se la lingua rilevata dai video del canale è già univoca, la Proposta di lingua viene da lì senza AI. Accettare usa le stesse operazioni della gestione manuale dei Gruppi e di `ChannelLanguages`.

## Testing Decisions

- Test JVM in `app/src/forkTest` su: costruzione delle richieste e interpretazione delle risposte e degli errori per i tre provider (con un server HTTP finto); filtro dei modelli `:free`; memoria delle chiavi (sigillo, round trip, tombstone); disponibilità della AI; merge di `fork_ai_keys` (convergenza, tombstone, peer che non dichiara la collezione); divisione in parti e unione; validazione dei timestamp; interpretazione del JSON di Riassunto video, Riassunto commenti e Proposte (compresi JSON rotti e Gruppi inesistenti); cache (chiave, sostituzione, limite).
- Verifica manuale sul telefono con ognuno dei tre provider, su un video breve, un video di oltre 2 ore e un video senza trascrizione; sulla Firestick con il fork dopo una Device Sync dal telefono; con un dispositivo con Flow originale, che la Device Sync resti compatibile.

## Out of Scope

- Trascrizione dell'audio (Whisper) per i video senza sottotitoli.
- Passaggio automatico a un altro provider in caso di limite.
- Funzioni che usano la cronologia o FlowNeuro, o che girano in background (Coda su misura, riassunti automatici dei nuovi video).
- Verifica del clickbait, Note generate dal video, ricerca in linguaggio naturale: possibili sviluppi successivi.
- Domande sul video sulla TV; inserimento delle chiavi sulla TV.
- Proposte di nuovi Sottogruppi.
- Provider oltre ai tre previsti, e modelli locali sul dispositivo.
