# Le richieste AI partono solo da un'azione esplicita e non includono mai cronologia o FlowNeuro

Flow promette che i dati di visione non lasciano il dispositivo, ma ogni richiesta a un Provider AI manda dati a terzi; i tier gratuiti (Google AI Studio, alcuni modelli OpenRouter) possono anche usare i prompt per l'addestramento. Per questo l'AI è disattivata di default, ogni richiesta parte solo da un'azione esplicita dell'utente su un contenuto preciso (un video, i suoi commenti, i canali da organizzare), niente gira in background, e la cronologia di visione e i dati di FlowNeuro non vengono mai inviati.

## Consequences

- Restano escluse funzioni che avrebbero bisogno della cronologia o di elaborazioni automatiche, per esempio una Coda "su misura" generata dall'AI o il riassunto automatico dei nuovi video delle Iscrizioni.
- Di regola si invia solo contenuto pubblico di YouTube: trascrizioni e commenti (senza i nomi degli autori).
- Eccezione dichiarata: l'organizzazione delle iscrizioni invia i canali seguiti che l'utente sceglie di analizzare (nome, descrizione, titoli recenti) e i nomi dei suoi Gruppi e Sottogruppi. Prima dell'invio una schermata mostra cosa parte; l'elenco completo delle iscrizioni non parte mai in automatico.
