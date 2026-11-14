# 14: Un solo YouTube Sync Device per gruppo

**What to build:** se un dispositivo del mio gruppo di Device Sync è già lo YouTube Sync Device, Flow non mi lascia collegare un account YouTube su un altro dispositivo dello stesso gruppo, e mi spiega perché.

**Blocked by:** nessuno

**Status:** needs-triage

## Problema

ADR 0002 vuole un solo YouTube Sync Device per gruppo: con due, la stessa modifica arriva a YouTube due volte e i video si duplicano nelle playlist. Oggi il fork blocca il collegamento solo sulle TV. Niente impedisce di collegare l'account su due telefoni (o un telefono e un tablet) dello stesso gruppo.

## Da decidere

- Il gruppo deve sapere quale dispositivo è lo YouTube Sync Device: serve un'informazione che viaggia con la Device Sync, senza cambiarne il protocollo (ADR 0002).
- Cosa succede quando lo YouTube Sync Device sparisce dal gruppo (telefono perso o cambiato) e un altro deve prenderne il posto.

## Comments

- 2026-11-13: emerso pianificando la verifica del ticket 12, quando la Firestick è passata al fork. Per ora l'unica protezione è il blocco sulle TV.
