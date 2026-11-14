# Spec: YouTube Sync

Status: ready-for-agent

Vedi anche: `GLOSSARY.md`, `docs/adr/0001-official-oauth-channel-for-youtube-sync.md`, `docs/adr/0002-youtube-sync-plugs-into-repositories-single-device.md`.

## Problem Statement

Uso Flow come client YouTube su telefono e Firestick, ma le mie iscrizioni e le mie playlist vivono anche sul mio account YouTube, dove le modifico dall'app ufficiale o dal browser. Oggi Flow non ha alcun legame con l'account: l'unica via è l'importazione manuale da Google Takeout, che è una tantum, lenta (ore di attesa per l'archivio) e solo in ingresso. Ogni volta che mi iscrivo a un canale o aggiungo un video a una playlist da una parte, l'altra resta indietro, e le modifiche fatte in Flow non arrivano mai su YouTube.

Il progetto Flow originale esclude qualsiasi login Google, quindi questa funzione deve vivere in un fork personale che resti comunque facile da riallineare al progetto originale.

## Solution

Un fork personale di Flow per Android (basato sulla variante FOSS, con un `applicationId` proprio) che aggiunge la **YouTube Sync**: una sincronizzazione bidirezionale e automatica di iscrizioni e playlist tra un profilo Flow e il suo **Linked Account**.

- Collego l'account una volta tramite il **canale ufficiale** (API ufficiale di YouTube con OAuth, nel browser). Il token serve solo alla YouTube Sync: riproduzione e navigazione restano anonime come oggi.
- Al primo collegamento Flow mi mostra un'anteprima dell'unione delle due liste e, dopo la mia conferma, porta entrambe le parti allo stesso stato senza cancellare nulla.
- Da lì in poi, ciò che cambio in Flow arriva su YouTube subito; ciò che cambio su YouTube arriva in Flow all'apertura dell'app, ogni 6 ore in background o quando premo "Sincronizza ora".
- Solo il telefono (il **YouTube Sync Device**) parla con YouTube. La Firestick, con Flow originale o con il fork, non collega mai l'account e riceve tutto tramite la **Device Sync** esistente.
- Protezioni: conferma prima di rimozioni di massa, elementi non rappresentabili mai toccati, modifiche in coda quando rete, quota o autorizzazione mancano.

## User Stories

### Collegamento dell'account

1. Come utente, voglio collegare il mio account YouTube a un profilo Flow dal browser, così da autorizzare la YouTube Sync senza usare i Google Play Services.
2. Come utente, voglio che Flow chieda solo i permessi necessari a leggere e modificare iscrizioni e playlist, così da limitare ciò che l'app può fare sul mio account.
3. Come utente, voglio vedere quale account è collegato al profilo corrente, così da sapere dove finiscono le mie modifiche.
4. Come utente con più profili Flow, voglio che il Linked Account sia per profilo, così che profili diversi possano collegare account diversi o nessuno.
5. Come utente, voglio che il collegamento non scada dopo pochi giorni, così da non dover rifare il login periodicamente.
6. Come utente, voglio scollegare l'account in qualsiasi momento, così da interrompere la YouTube Sync.
7. Come utente che scollega l'account, voglio che iscrizioni e playlist restino in Flow come dati normali, così da non perdere nulla.
8. Come utente che scollega l'account, voglio che le Owned Playlist diventino playlist Flow-only, così da poterle continuare a usare.
9. Come utente che scollega l'account, voglio che il permesso venga revocato e la Sync Baseline eliminata, così che non resti nulla di attivo verso Google.
10. Come utente che scollega l'account, voglio che su YouTube non venga toccato nulla, così che scollegare sia un'operazione sicura.
11. Come utente, voglio che la riproduzione e la navigazione non usino mai il Linked Account, così che il mio account Google non sia legato all'estrazione non ufficiale dei video.

### Primo collegamento

12. Come utente al primo collegamento, voglio un'anteprima di cosa verrà aggiunto da ciascuna parte (es. "+12 su YouTube, +40 su Flow"), così da decidere con cognizione di causa.
13. Come utente al primo collegamento, voglio che le due liste di iscrizioni vengano unite senza cancellare nulla da nessuna parte, così da non perdere iscrizioni importate da Takeout o fatte su YouTube.
14. Come utente al primo collegamento, voglio che tutte le Owned Playlist di YouTube arrivino in Flow, così da trovarle subito.
15. Come utente al primo collegamento, voglio che le Saved Playlist arrivino in Flow in sola lettura, così da poterle guardare senza poterle modificare per errore.
16. Come utente al primo collegamento, voglio che le playlist create in Flow restino Flow-only, così da non riempire YouTube di playlist di prova o di musica.
17. Come utente al primo collegamento, voglio essere avvisato nell'anteprima quando una playlist Flow ha lo stesso nome di una Owned Playlist, così da notare possibili doppioni senza che vengano unite automaticamente.
18. Come utente, voglio poter annullare il primo collegamento dall'anteprima, così che nulla venga applicato se l'unione non mi convince.
19. Come utente che cambia telefono, voglio che il nuovo YouTube Sync Device si comporti come al primo collegamento (unione con anteprima), così che una Sync Baseline mancante non causi cancellazioni.

### Iscrizioni

20. Come utente, voglio che quando mi iscrivo a un canale in Flow l'iscrizione venga creata anche su YouTube, così da ritrovarla nell'app ufficiale.
21. Come utente, voglio che quando mi disiscrivo in Flow la disiscrizione avvenga anche su YouTube.
22. Come utente, voglio che le iscrizioni fatte su YouTube compaiano in Flow alla successiva YouTube Sync.
23. Come utente, voglio che le disiscrizioni fatte su YouTube vengano applicate in Flow alla successiva YouTube Sync.
24. Come utente, voglio che un'iscrizione già presente su entrambi i lati non venga duplicata né causi errori.
25. Come utente, voglio che i gruppi di iscrizioni restino Flow-only, così da organizzarli liberamente senza effetti su YouTube.

### Owned Playlist

26. Come utente, voglio collegare a YouTube una playlist creata in Flow, così che diventi una Owned Playlist.
27. Come utente che collega una playlist Flow, voglio che venga sempre creata una nuova playlist su YouTube, così da evitare unioni ambigue per titolo.
28. Come utente, voglio che le playlist create su YouTube da Flow siano private di default, così da non pubblicare nulla per errore.
29. Come utente, voglio che aggiungere un video a una Owned Playlist in Flow lo aggiunga anche su YouTube.
30. Come utente, voglio che rimuovere un video da una Owned Playlist in Flow lo rimuova anche su YouTube.
31. Come utente, voglio che riordinare una Owned Playlist in Flow riordini anche quella su YouTube.
32. Come utente, voglio che rinominare una Owned Playlist in Flow la rinomini anche su YouTube.
33. Come utente, voglio che eliminare una Owned Playlist in Flow la elimini anche su YouTube.
34. Come utente, voglio che le modifiche fatte su YouTube a una Owned Playlist (video, ordine, titolo, eliminazione) arrivino in Flow.
35. Come utente, voglio che una nuova playlist creata su YouTube compaia in Flow come Owned Playlist.
36. Come utente, voglio che visibilità e descrizione delle playlist esistenti su YouTube non vengano toccate da Flow.

### Saved Playlist

37. Come utente, voglio che le playlist di altri che ho salvato su YouTube compaiano in Flow come Saved Playlist.
38. Come utente, voglio che le Saved Playlist si aggiornino quando il loro autore le modifica.
39. Come utente, voglio che Flow non mi permetta di modificare una Saved Playlist, così da non generare scritture impossibili verso YouTube.

### Conflitti

40. Come utente, voglio che una modifica fatta da una sola parte dall'ultima YouTube Sync venga sempre propagata all'altra.
41. Come utente, voglio che quando una parte elimina un elemento e l'altra lo modifica, prevalga l'eliminazione.
42. Come utente, voglio che quando entrambe le parti modificano lo stesso elemento in modo incompatibile (es. due ordinamenti diversi, due titoli diversi), vinca YouTube.
43. Come utente, voglio che un elemento aggiunto da entrambe le parti risulti presente una sola volta.

### Protezioni

44. Come utente, voglio che se una YouTube Sync rimuoverebbe più del 10% degli elementi o più di 20 elementi, Flow si fermi e mi chieda conferma, così che un bug o una perdita di dati locali non mi disiscriva da tutto.
45. Come utente che rifiuta una rimozione di massa, voglio che non venga applicato nulla di quella YouTube Sync e che la Sync Baseline resti invariata.
46. Come utente, voglio che i video eliminati o privati in una playlist YouTube vengano ignorati in entrambe le direzioni: non mostrati in Flow e mai rimossi da YouTube.
47. Come utente, voglio che la Sync Baseline non viaggi con la Device Sync, così che uno stato vecchio arrivato da un altro dispositivo non possa causare cancellazioni.

### Tempistiche

48. Come utente, voglio che le modifiche fatte in Flow vengano inviate a YouTube subito.
49. Come utente offline, voglio che le modifiche fatte in Flow restino in coda e partano appena torna la rete.
50. Come utente, voglio che Flow legga le novità da YouTube all'apertura dell'app.
51. Come utente, voglio che Flow legga le novità da YouTube in background ogni 6 ore.
52. Come utente, voglio un pulsante "Sincronizza ora", così da forzare la YouTube Sync quando mi serve.
53. Come utente, voglio un indicatore discreto con lo stato e l'ora dell'ultima YouTube Sync riuscita.

### Errori

54. Come utente, voglio che se la quota giornaliera dell'API è esaurita le modifiche restino in coda e si riprovi il giorno dopo, senza notifiche.
55. Come utente, voglio una notifica "Ricollega l'account" se l'autorizzazione viene revocata o scade, così da sapere che la YouTube Sync è ferma.
56. Come utente, voglio che un errore su un singolo elemento non blocchi la sincronizzazione degli altri.
57. Come utente, voglio che le modifiche in coda non vadano perse se l'app viene chiusa o il telefono riavviato.

### Più dispositivi

58. Come utente con telefono e Firestick, voglio che solo il telefono esegua la YouTube Sync, così che le modifiche non vengano inviate due volte a YouTube.
59. Come utente, voglio che una modifica fatta sulla Firestick arrivi al telefono tramite la Device Sync e da lì a YouTube.
60. Come utente, voglio che le novità lette da YouTube arrivino alla Firestick tramite la Device Sync.
61. Come utente, voglio poter usare sulla Firestick sia Flow originale sia il fork; con il fork, la Firestick non mi lascia collegare l'account e mi dice di farlo dal telefono.
62. Come utente, voglio che i dati Flow-only (gruppi, preferiti, note, like) continuino a viaggiare tra i miei dispositivi con la Device Sync, senza mai andare su YouTube.

### Fork

63. Come utente, voglio che il fork si installi accanto a Flow originale con un `applicationId` proprio.
64. Come utente, voglio trasferire i miei dati da Flow originale al fork con il backup completo esistente.
65. Come manutentore del fork, voglio che il codice della YouTube Sync sia isolato e tocchi il meno possibile il codice originale, così da poter integrare facilmente gli aggiornamenti del progetto originale.
66. Come manutentore del fork, voglio compilare l'APK localmente o con GitHub Actions sul mio fork.

## Implementation Decisions

- **Base del fork**: variante FOSS di Flow per Android, con `applicationId` proprio. Sulla Firestick può girare Flow originale o il fork; la Device Sync resta compatibile perché il suo protocollo non viene modificato. Su una TV il fork blocca il collegamento dell'account (ADR 0002).
- **Modulo YouTube Sync isolato**: tutto il codice nuovo vive in un'area separata. Le modifiche al codice originale si limitano a: punti di aggancio per accodare le modifiche locali, voci di interfaccia (collegamento, anteprima, stato, "Sincronizza ora"), registrazione del lavoro in background e del redirect OAuth.
- **Integrazione con gli archivi di Flow** (ADR 0002): la YouTube Sync legge e scrive iscrizioni e playlist tramite la stessa interfaccia di accesso ai dati che usa la Device Sync. Le scritture provenienti da YouTube appaiono come modifiche locali e la Device Sync le propaga da sola. Non si estende il protocollo della Device Sync.
- **Interfaccia `YouTubeAccount`**: un'astrazione dell'account remoto con le operazioni necessarie, cioè elencare, aggiungere e rimuovere iscrizioni; elencare le playlist distinguendo Owned Playlist e Saved Playlist; creare, rinominare ed eliminare playlist; elencare, aggiungere, rimuovere e riposizionare i video di una playlist. Gli errori sono tipizzati: quota esaurita, autorizzazione non valida, elemento non trovato, duplicato, errore transitorio. L'implementazione reale usa la YouTube Data API v3; i test usano una versione in memoria.
- **Canale ufficiale** (ADR 0001): login OAuth con AppAuth nel browser, senza Google Play Services. Si usa un progetto Google Cloud personale (per ora in "Testing", con refresh token di 7 giorni: vedi ADR 0001), e i permessi richiesti sono quelli minimi per gestire iscrizioni e playlist. I token sono salvati in modo cifrato, per profilo. Il client HTTP del canale ufficiale è separato da quello usato per riproduzione e navigazione e non condivide cookie né intestazioni.
- **Canale cookie**: non implementato. L'interfaccia `YouTubeAccount` permette di aggiungerlo in futuro senza cambiare il motore di sincronizzazione.
- **Motore di confronto a tre vie**: per ogni collezione (iscrizioni, insieme delle Owned Playlist, contenuto e ordine di ciascuna Owned Playlist, titolo) si confrontano lo stato di Flow, lo stato di YouTube e la Sync Baseline. Le regole sono:
  - una modifica fatta da una sola parte si propaga all'altra;
  - un'eliminazione prevale su una modifica;
  - tra due modifiche incompatibili vince YouTube;
  - un'aggiunta fatta da entrambe le parti diventa un solo elemento.

  Il risultato è un piano di operazioni verso Flow e verso YouTube, calcolato prima di qualsiasi scrittura. Questo permette l'anteprima e il controllo sulle rimozioni di massa.
- **Primo collegamento** (nessuna Sync Baseline): il piano è un'unione senza rimozioni. Le Owned Playlist e le Saved Playlist arrivano in Flow, mentre le playlist Flow restano Flow-only. Prima di applicarlo viene mostrata un'anteprima con i conteggi e gli avvisi sui titoli uguali.
- **Protezione dalle rimozioni di massa**: se il piano rimuove più di 20 elementi di una collezione, o più del 10% purché siano almeno 3, si chiede conferma all'utente. Se l'utente rifiuta, non si applica nulla e la Sync Baseline resta invariata.
- **Elementi non rappresentabili**: i video eliminati o privati nelle playlist YouTube restano fuori sia dal confronto sia dalle scritture. Non compaiono mai nel piano.
- **Sync Baseline**: salvata solo sul YouTube Sync Device, per profilo, separata dai dati sincronizzati con la Device Sync. Viene aggiornata solo dopo che il piano è stato applicato con successo; per gli elementi falliti si aggiorna elemento per elemento, così che si ritentino.
- **Coda delle modifiche locali** (ADR 0003): le modifiche di Flow verso YouTube passano da una coda persistente, che è la differenza tra Flow e la Sync Baseline. Le modifiche consecutive sullo stesso elemento si compattano, per esempio un'iscrizione seguita da una disiscrizione si annulla. La coda sopravvive a riavvii e blocchi dell'app.
- **Tempistiche**: le modifiche locali partono subito, oppure appena torna la rete. Le novità da YouTube si leggono all'apertura dell'app, con un lavoro in background ogni 6 ore che richiede la rete, e con il pulsante "Sincronizza ora". Quando la quota è esaurita si riprova dopo il reset giornaliero. Con l'autorizzazione non valida la YouTube Sync si ferma e compare la notifica "Ricollega l'account".
- **YouTube Sync Device**: la YouTube Sync gira solo dove il profilo ha un Linked Account, cioè sul telefono. Un dispositivo senza Linked Account non fa nulla verso YouTube.
- **Owned Playlist e Saved Playlist in Flow**: ogni playlist sincronizzata conserva il proprio ID YouTube e il proprio tipo. Le Saved Playlist sono in sola lettura nell'interfaccia. Collegare una playlist Flow-only crea sempre una nuova playlist privata su YouTube e ne salva l'ID. Il titolo, l'elenco dei video e l'ordine si sincronizzano; visibilità e descrizione non vengono mai scritte, tranne la visibilità privata alla creazione.
- **Scollegamento**: il permesso viene revocato e i token e la Sync Baseline vengono eliminati. Le Owned Playlist e le Saved Playlist diventano playlist Flow-only. La coda viene svuotata senza inviare nulla, e su YouTube non si tocca niente.
- **Quota**: le letture si fanno a pagine per contenere i costi. Ogni scrittura costa circa 50 unità, su un budget giornaliero di 10.000.

## Testing Decisions

- **Cos'è un buon test**: verifica il comportamento osservabile dall'esterno, cioè lo stato finale di Flow e di YouTube dopo una YouTube Sync, e non i dettagli interni come la forma del piano o le classi intermedie. I test sopravvivono a un refactoring interno del motore.
- **Seam principale (unico per la logica)**: l'intera YouTube Sync. Il test prepara gli archivi di Flow tramite l'interfaccia di accesso ai dati esistente, un `YouTubeAccount` in memoria e una Sync Baseline, esegue una YouTube Sync e verifica lo stato finale di Flow, del finto account e della Sync Baseline. Da qui si coprono:
  - il confronto a tre vie e le regole sui conflitti;
  - il primo collegamento con unione e anteprima;
  - la protezione dalle rimozioni di massa, sia confermata sia rifiutata;
  - gli elementi non rappresentabili;
  - Owned Playlist e Saved Playlist, compreso il collegamento di una playlist Flow-only;
  - la coda e la compattazione delle modifiche;
  - quota esaurita, autorizzazione non valida ed errori su singoli elementi;
  - lo scollegamento.
- **Seam secondario**: l'adattatore della YouTube Data API, testato contro un finto server HTTP che riproduce le risposte di Google. Copre paginazione, quota esaurita, autorizzazione revocata, duplicati ed elementi non trovati, e serve a verificare la traduzione nei tipi di errore di `YouTubeAccount`.
- **Precedenti nel codice**: i test della Device Sync usano finti server in memoria e loopback (`StandInServer`, `WebDavStandIn`, `SyncLoopback`, `LinkedGroup`) e test di convergenza e idempotenza del merge (`MergeConvergenceTest`, `SyncIdempotencyTest`, `PlaylistReconcileTest`). I test della YouTube Sync seguono lo stesso stile: test JVM puri, senza dispositivo.
- **Proprietà da verificare oltre agli esempi**: l'**idempotenza** (due YouTube Sync consecutive senza modifiche non producono operazioni) e la **convergenza** (dopo una YouTube Sync riuscita Flow e YouTube coincidono sulle collezioni sincronizzate, esclusi gli elementi non rappresentabili).
- **Verifica manuale**: il login con AppAuth nel browser, l'interfaccia (anteprima, indicatore di stato, notifica) e un giro reale con l'account personale e la Firestick.

## Out of Scope

- Cronologia visualizzazioni, "Guarda più tardi" e video piaciuti di YouTube: non sono disponibili tramite il canale ufficiale. Per ora resta l'importazione manuale da Takeout.
- Il canale cookie (InnerTube con cookie di sessione).
- Flow Desktop.
- La sincronizzazione di visibilità e descrizione delle playlist.
- L'unione automatica delle playlist per titolo.
- La modifica del protocollo della Device Sync o il passaggio della Sync Baseline tramite la Device Sync.
- Il login per riproduzione e navigazione, per esempio per contenuti riservati agli abbonati o per superare i controlli anti-bot.
- La distribuzione pubblica del fork e la verifica dell'app da parte di Google.
- La proposta della funzione al progetto originale.

## Further Notes

- Il maintainer del progetto originale considera il login Google fuori ambito (issue chiuse come not_planned; PR di login non accettata). La funzione resterà solo nel fork.
- Licenza GPL-3.0: il fork deve restare GPL-3.0 e conservare le note di copyright originali.
- Il ritardo massimo di una modifica fatta sulla Firestick è circa l'intervallo della Device Sync (un'ora) più quello della YouTube Sync, a meno di usare "Sincronizza ora".
- Il progetto Google Cloud in "Production" non verificato mostrerebbe una schermata di avviso al primo consenso: è previsto e accettato. Finché resta in "Testing", l'account va ricollegato ogni 7 giorni.
- Le Saved Playlist non arrivano con il Canale ufficiale: la YouTube Data API non permette di leggere le playlist salvate nella raccolta (issue Google 79957181). Il motore le gestisce già; arriveranno con il Canale cookie.
