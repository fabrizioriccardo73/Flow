# 02: Lingua del canale

**What to build:** assegno una lingua, o nessuna, a un canale dal foglio dei gruppi del canale; la scelta resta salvata per profilo.

**Blocked by:** nessuno

**Status:** ready-for-agent

- [x] Riga "Lingua" in `ChannelGroupSheet` con l'elenco delle lingue e la voce "Nessuna (rileva dal video)"
- [x] Memoria per profilo `channelId → codice ISO 639-1`, con HLC per voce e tombstone alla rimozione
- [x] La voce resta anche dopo la disiscrizione dal canale
- [ ] Test JVM sulla memoria (assegna, cambia, togli, round trip)

## Comments

**2026-11-14**: Implementato, non ancora provato sul telefono.
- `subsfilters/language/ChannelLanguages.kt`: DataStore `fork_channel_languages` (`PROFILE_LIBRARY`), elenco di `ChannelLanguageRecord` con HLC; togliere la lingua lascia un tombstone. Lo stamp usa `DeviceIdentity.editTime()`.
- `subsfilters/ui/ChannelLanguageRow.kt`: riga "Lingua" in testa al foglio dei gruppi, con scelta tra "Rileva da ogni video" e le lingue di `Languages.ASSIGNABLE`. Nascosta sulla TV.
- Originale: `SaveToCollectionSheet` ha un parametro `header` opzionale, `ChannelGroupSheet` gli passa la riga (righe `// fork: Filtri iscrizioni`).
- Test della memoria: coperti dal merge (`ChannelLanguageRecordsTest`); la memoria su DataStore non ha un test JVM proprio.
