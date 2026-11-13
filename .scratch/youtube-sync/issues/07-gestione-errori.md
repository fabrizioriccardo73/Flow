# 07: Gestione degli errori

**What to build:** la YouTube Sync si comporta bene quando qualcosa va storto: con la quota esaurita aspetta il giorno dopo senza disturbarmi, con l'autorizzazione non valida si ferma e mi chiede di ricollegare l'account, e un elemento problematico non blocca gli altri.

**Blocked by:** 06 (Coda delle modifiche e tempistiche)

**Status:** done (da verificare sul telefono, ticket 12)

- [x] Quota esaurita: le modifiche restano in coda e si riprova dopo il reset giornaliero della quota, senza notifiche
- [x] Autorizzazione revocata o scaduta: la YouTube Sync si ferma, compare la notifica "Ricollega l'account", l'indicatore di stato lo riflette; ricollegando riparte da dove era
- [x] Errore su un singolo elemento (non trovato, duplicato): gli altri elementi procedono; i duplicati sono trattati come successo
- [x] Errori transitori: nuovo tentativo con attesa crescente
- [x] Test dell'adattatore contro un finto server HTTP che riproduce le risposte di Google: paginazione, quota esaurita, autorizzazione revocata, duplicato, non trovato, errore transitorio → tipi di errore di `YouTubeAccount`
- [x] Test sul seam principale per ciascun tipo di errore

## Comments

- Quota esaurita: la YouTube Sync riprova dopo il reset di mezzanotte (ora del Pacifico) più 10 minuti, senza notifiche. Lo stato mostra l'ora della ripresa.
- Autorizzazione non valida: la YouTube Sync si ferma, compare la notifica "Ricollega l'account" e lo stato lo indica (toccandolo si ricollega). Ricollegando lo stesso account la Sync Baseline resta e si riparte da dove ci si era fermati. Con un altro account si riparte come al primo collegamento.
- Errori transitori: WorkManager riprova con attesa esponenziale (da 60 secondi, al massimo 8 tentativi), poi resta la lettura periodica.
- Test: `YouTubeDataApiAccountTest` (12 test, con risposte di Google simulate) e `YouTubeSyncRunnerTest` (un caso per ogni tipo di errore).
