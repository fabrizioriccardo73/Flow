# 13: Playlist doppie dopo scollegamento e ricollegamento

**What to build:** quando scollego il Linked Account e poi lo ricollego, non voglio ritrovarmi in Flow ogni Owned Playlist e Saved Playlist due volte.

**Blocked by:** nessuno

**Status:** needs-triage

## Problema

Allo scollegamento (ticket 11) ogni Owned Playlist e Saved Playlist diventa una playlist Flow-only con un nuovo ID e perde il legame con YouTube. Al ricollegamento si riparte come al primo collegamento: le stesse playlist arrivano di nuovo da YouTube. In Flow ci sono quindi due copie di ogni playlist, la Flow-only e quella collegata, e le copie viaggiano anche sugli altri dispositivi con la Device Sync. L'anteprima mostra l'avviso sui titoli uguali, ma non unisce nulla (spec, storia 17).

## Da decidere

- Se valga la pena risolverlo, visto che scollegare e ricollegare è raro.
- Possibili strade: ricordare l'ID YouTube di origine nelle playlist liberate e proporre di ricollegarle nell'anteprima; oppure chiedere allo scollegamento se tenere le copie.

## Comments

- 2026-11-13: emerso pianificando la verifica del ticket 12. Per ora le copie Flow-only si cancellano a mano dopo il ricollegamento.
