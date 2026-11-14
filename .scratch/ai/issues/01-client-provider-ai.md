# 01: Client dei Provider AI

**What to build:** un unico client compatibile OpenAI per Google AI Studio, Groq e OpenRouter: lista dei modelli, chat completion, errori tradotti in un tipo del fork.

**Blocked by:** nessuno

**Status:** ready-for-agent

- [ ] Enum dei Provider AI con base URL, header e link alla pagina della chiave (vedi spec)
- [ ] `GET models` → lista di modelli con id, nome e budget di contesto (dalla risposta se c'è, altrimenti un valore prudente); filtro `:free` per OpenRouter
- [ ] `POST chat/completions` con messaggi di sistema/utente e richiesta di risposta JSON dove serve
- [ ] Errori: chiave non valida (401/403), limite (429 con `Retry-After`), contesto troppo lungo, rete, altro
- [ ] Modello predefinito per provider in una costante del fork, scelto tra i gratuiti con contesto ampio
- [ ] Solo OkHttp e kotlinx.serialization, nessun SDK; codice in `app/src/fork/java/io/github/aedev/flow/ai/`
- [ ] Test JVM con un server HTTP finto: richieste per i tre provider, parsing dei modelli, filtro `:free`, mappatura degli errori
