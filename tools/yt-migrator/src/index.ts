#!/usr/bin/env node
import { Command } from 'commander';
import * as p from '@clack/prompts';
import color from 'picocolors';
import { StorageManager } from './storage.js';
import { authenticateAccount, ensureTargetChannelExists } from './auth.js';
import { fetchSubscriptions, fetchPlaylists } from './scanner.js';
import {
  migrateSubscriptions,
  migratePlaylists,
  type PlaylistConflictStrategy
} from './migrator.js';
import type {
  CLIOptions,
  ChannelSubscription,
  UserPlaylist,
  MigrationCheckpoint
} from './types.js';

const program = new Command();

program
  .name('yt-migrator')
  .description('CLI evoluta per copiare canali seguiti e playlist da un account YouTube ad un altro')
  .version('1.0.0')
  .option('-d, --dry-run', 'Simula l\'intera procedura senza effettuare modifiche reali')
  .option('-r, --resume', 'Riprende l\'ultima migrazione dal checkpoint salvato')
  .option('--only-subscriptions', 'Migra solo i canali seguiti (iscrizioni)')
  .option('--only-playlists', 'Migra solo le playlist create')
  .option('--delay <ms>', 'Ritardo base in millisecondi tra le richieste (default: 1000)', '1000')
  .option('--reset-auth', 'Cancella le credenziali memorizzate e richiede un nuovo accesso')
  .option('--data-dir <path>', 'Percorso cartella dati per token e checkpoint');

program.parse(process.argv);

const cliFlags = program.opts();

const options: CLIOptions = {
  dryRun: Boolean(cliFlags.dryRun),
  resume: Boolean(cliFlags.resume),
  onlySubscriptions: Boolean(cliFlags.onlySubscriptions),
  onlyPlaylists: Boolean(cliFlags.onlyPlaylists),
  delay: parseInt(cliFlags.delay, 10) || 1000,
  resetAuth: Boolean(cliFlags.resetAuth),
  dataDir: cliFlags.dataDir
};

async function main() {
  console.clear();
  p.intro(color.bgCyan(color.black(' 🎬 YOUTUBE ACCOUNT MIGRATOR ')));

  if (options.dryRun) {
    p.log.warn(color.bold(color.yellow('MODALITÀ ANTEPRIMA (DRY-RUN) ATTIVA: Nessuna modifica verrà applicata su YouTube.')));
  }

  const storage = new StorageManager(options.dataDir);

  if (options.resetAuth) {
    storage.clearCredentials();
    p.log.info(color.green('Credenziali precedenti rimosse.'));
  }

  // 1. Check for existing checkpoint
  let checkpoint: MigrationCheckpoint | null = storage.loadCheckpoint();
  if (checkpoint && !options.resume) {
    const shouldResume = await p.confirm({
      message: `Trovata migrazione precedente del ${new Date(checkpoint.updatedAt).toLocaleString()}. Vuoi riprenderla?`,
      initialValue: true
    });

    if (p.isCancel(shouldResume)) {
      p.cancel('Operazione annullata.');
      process.exit(0);
    }

    if (!shouldResume) {
      storage.clearCheckpoint();
      checkpoint = null;
    } else {
      options.resume = true;
    }
  }

  // 2. Authentication Step
  p.log.step(color.bold('Fase 1: Connessione degli account YouTube'));

  const sourceClient = await authenticateAccount(
    'source',
    'Account A (SORGENTE - Da cui copiare)',
    storage,
    options.resetAuth
  );
  p.log.success(
    `Account A connesso: ${color.cyan(sourceClient.profile.name || 'Sconosciuto')} ${
      sourceClient.profile.email ? `(${sourceClient.profile.email})` : ''
    }`
  );

  const targetClient = await authenticateAccount(
    'target',
    'Account B (DESTINAZIONE - Su cui copiare)',
    storage,
    options.resetAuth
  );
  p.log.success(
    `Account B connesso: ${color.magenta(targetClient.profile.name || 'Sconosciuto')} ${
      targetClient.profile.email ? `(${targetClient.profile.email})` : ''
    }`
  );

  // Ensure Account B has a YouTube channel created (required by YouTube to manage playlists)
  if (!options.onlySubscriptions) {
    await ensureTargetChannelExists(targetClient);
  }

  // Safety check
  if (
    sourceClient.profile.name === targetClient.profile.name &&
    sourceClient.profile.email &&
    sourceClient.profile.email === targetClient.profile.email
  ) {
    const proceedAnyway = await p.confirm({
      message: color.red(
        'Attenzione: L\'Account Sorgente e l\'Account Destinazione sembrano identici! Vuoi continuare comunque?'
      ),
      initialValue: false
    });
    if (p.isCancel(proceedAnyway) || !proceedAnyway) {
      p.cancel('Operazione annullata per motivi di sicurezza.');
      process.exit(0);
    }
  }

  // Initialize checkpoint if fresh
  if (!checkpoint) {
    checkpoint = {
      version: 1,
      updatedAt: new Date().toISOString(),
      sourceAccount: sourceClient.profile,
      targetAccount: targetClient.profile,
      subscriptions: { completed: [], skipped: [], failed: [] },
      playlists: { completed: {}, skipped: [], failed: [] }
    };
    storage.saveCheckpoint(checkpoint);
  }

  // 3. Scanning Data
  p.log.step(color.bold('Fase 2: Scansione contenuti degli account'));

  const spinner = p.spinner();

  spinner.start('Lettura canali seguiti da Account A...');
  const sourceSubscriptions = options.onlyPlaylists
    ? []
    : await fetchSubscriptions(sourceClient.yt, (count) => {
        spinner.message(`Lettura canali seguiti da Account A... (${count} trovati)`);
      });
  spinner.stop(`Account A: ${color.bold(sourceSubscriptions.length)} canali seguiti trovati.`);

  spinner.start('Lettura canali già seguiti da Account B...');
  const targetSubscriptions = options.onlyPlaylists
    ? []
    : await fetchSubscriptions(targetClient.yt);
  const targetSubIds = new Set(targetSubscriptions.map((s) => s.id));
  spinner.stop(`Account B: ${color.bold(targetSubscriptions.length)} canali già seguiti.`);

  spinner.start('Lettura playlist da Account A...');
  const sourcePlaylists = options.onlySubscriptions
    ? []
    : await fetchPlaylists(sourceClient.yt, (count) => {
        spinner.message(`Lettura playlist da Account A... (${count} trovate)`);
      });
  spinner.stop(`Account A: ${color.bold(sourcePlaylists.length)} playlist create trovate.`);

  spinner.start('Lettura playlist esistenti su Account B...');
  const targetPlaylists = options.onlySubscriptions
    ? []
    : await fetchPlaylists(targetClient.yt);
  spinner.stop(`Account B: ${color.bold(targetPlaylists.length)} playlist esistenti.`);

  // 4. Granularity / Selection
  let selectedChannels: ChannelSubscription[] = [];
  if (!options.onlyPlaylists && sourceSubscriptions.length > 0) {
    const subChoice = await p.select({
      message: 'Come desideri gestire i canali seguiti?',
      options: [
        {
          value: 'all',
          label: `Copia tutti i canali seguiti (${sourceSubscriptions.length})`,
          hint: 'I canali già presenti su Account B verranno saltati automaticamente'
        },
        {
          value: 'select',
          label: 'Seleziona manualmente i canali da copiare'
        },
        {
          value: 'none',
          label: 'Salta la migrazione dei canali seguiti'
        }
      ]
    });

    if (p.isCancel(subChoice)) {
      p.cancel('Operazione annullata.');
      process.exit(0);
    }

    if (subChoice === 'all') {
      selectedChannels = sourceSubscriptions;
    } else if (subChoice === 'select') {
      const selectedIds = await p.multiselect({
        message: 'Seleziona i canali da copiare:',
        options: sourceSubscriptions.map((ch) => ({
          value: ch.id,
          label: ch.title,
          hint: targetSubIds.has(ch.id) ? color.yellow('(già seguito su B)') : undefined
        })),
        required: false
      });

      if (p.isCancel(selectedIds)) {
        p.cancel('Operazione annullata.');
        process.exit(0);
      }

      const idSet = new Set(selectedIds as string[]);
      selectedChannels = sourceSubscriptions.filter((ch) => idSet.has(ch.id));
    }
  }

  let selectedPlaylists: UserPlaylist[] = [];
  let playlistConflictStrategy: PlaylistConflictStrategy = 'merge';

  if (!options.onlySubscriptions && sourcePlaylists.length > 0) {
    const plChoice = await p.select({
      message: 'Come desideri gestire le playlist create?',
      options: [
        {
          value: 'all',
          label: `Copia tutte le playlist (${sourcePlaylists.length})`
        },
        {
          value: 'select',
          label: 'Seleziona manualmente le playlist da copiare'
        },
        {
          value: 'none',
          label: 'Salta la migrazione delle playlist'
        }
      ]
    });

    if (p.isCancel(plChoice)) {
      p.cancel('Operazione annullata.');
      process.exit(0);
    }

    if (plChoice === 'all') {
      selectedPlaylists = sourcePlaylists;
    } else if (plChoice === 'select') {
      const selectedIds = await p.multiselect({
        message: 'Seleziona le playlist da copiare:',
        options: sourcePlaylists.map((pl) => ({
          value: pl.id,
          label: pl.title,
          hint: pl.videoCount !== undefined ? `${pl.videoCount} video` : undefined
        })),
        required: false
      });

      if (p.isCancel(selectedIds)) {
        p.cancel('Operazione annullata.');
        process.exit(0);
      }

      const idSet = new Set(selectedIds as string[]);
      selectedPlaylists = sourcePlaylists.filter((pl) => idSet.has(pl.id));
    }

    if (selectedPlaylists.length > 0) {
      const strategyChoice = await p.select({
        message: 'Se su Account B esiste già una playlist con lo stesso titolo:',
        options: [
          {
            value: 'merge',
            label: 'Unisci (Recommended)',
            hint: 'Aggiunge alla playlist esistente solo i video mancanti'
          },
          {
            value: 'duplicate',
            label: 'Crea nuova playlist duplicata',
            hint: 'Crea una seconda playlist con lo stesso nome'
          },
          {
            value: 'skip',
            label: 'Salta playlist duplicata',
            hint: 'Non tocca la playlist già esistente'
          }
        ]
      });

      if (p.isCancel(strategyChoice)) {
        p.cancel('Operazione annullata.');
        process.exit(0);
      }

      playlistConflictStrategy = strategyChoice as PlaylistConflictStrategy;
    }
  }

  // 5. Summary and Confirmation
  const toSubscribeCount = selectedChannels.filter((c) => !targetSubIds.has(c.id)).length;
  const alreadySubscribedCount = selectedChannels.length - toSubscribeCount;

  const estimatedMinutes = Math.max(
    1,
    Math.round(
      ((toSubscribeCount * (options.delay! + 250)) +
        (selectedPlaylists.length * (options.delay! + 500))) /
        60000
    )
  );

  p.note(
    [
      `${color.bold('Account Sorgente (A):')} ${sourceClient.profile.name || 'Account A'}`,
      `${color.bold('Account Destinazione (B):')} ${targetClient.profile.name || 'Account B'}`,
      `----------------------------------------`,
      `${color.bold('Canali selezionati:')} ${selectedChannels.length}`,
      `  • Da iscrivere su B: ${color.green(toSubscribeCount)}`,
      `  • Già seguiti su B (verranno saltati): ${color.yellow(alreadySubscribedCount)}`,
      `${color.bold('Playlist selezionate:')} ${color.cyan(selectedPlaylists.length)}`,
      `${color.bold('Strategia playlist:')} ${playlistConflictStrategy}`,
      `${color.bold('Tempo stimato:')} ~${estimatedMinutes} min (pausa anti-spam di ${options.delay}ms)`,
      `${color.bold('Modalità:')} ${options.dryRun ? color.yellow('DRY-RUN (Simulazione)') : color.green('SCRITTURA REALE')}`
    ].join('\n'),
    'Riepilogo Migrazione'
  );

  const confirmStart = await p.confirm({
    message: options.dryRun
      ? 'Vuoi avviare la simulazione?'
      : 'Vuoi avviare la migrazione adesso?',
    initialValue: true
  });

  if (p.isCancel(confirmStart) || !confirmStart) {
    p.cancel('Migrazione annullata.');
    process.exit(0);
  }

  // 6. Execution
  p.log.step(color.bold('Fase 3: Esecuzione del trasferimento'));

  let subResult = { migrated: 0, skipped: 0, failed: 0 };
  if (selectedChannels.length > 0) {
    p.log.info(color.bold(`Trasferimento iscrizioni (${selectedChannels.length} totali)...`));

    subResult = await migrateSubscriptions(
      targetClient.yt,
      selectedChannels,
      targetSubIds,
      checkpoint,
      storage,
      options,
      {
        onSubscriptionProgress: (cur, tot, ch, st) => {
          const prefix = `[${cur}/${tot}]`;
          if (st === 'migrated') {
            p.log.success(`${prefix} Iscritto a: ${color.bold(ch.title)}`);
          } else if (st === 'skipped') {
            p.log.warn(`${prefix} Saltato (già presente): ${ch.title}`);
          } else {
            p.log.error(`${prefix} Errore iscrizione a: ${ch.title}`);
          }
        }
      }
    );
  }

  let plResult = { migrated: 0, skipped: 0, failed: 0 };
  if (selectedPlaylists.length > 0) {
    p.log.info(color.bold(`Trasferimento playlist (${selectedPlaylists.length} totali)...`));

    plResult = await migratePlaylists(
      sourceClient.yt,
      targetClient.yt,
      targetClient.ytWriter,
      selectedPlaylists,
      targetPlaylists,
      playlistConflictStrategy,
      checkpoint,
      storage,
      options,
      {
        onPlaylistProgress: (cur, tot, pl, st, vCount) => {
          const prefix = `[${cur}/${tot}]`;
          if (st === 'migrated') {
            p.log.success(`${prefix} Copiata playlist: ${color.bold(pl.title)} (${vCount ?? 0} video)`);
          } else if (st === 'skipped') {
            p.log.warn(`${prefix} Saltata playlist: ${pl.title}`);
          } else {
            p.log.error(`${prefix} Errore copia playlist: ${pl.title}`);
          }
        }
      }
    );
  }

  // 7. Completed Report
  p.note(
    [
      `${color.bold('Iscrizioni canali:')}`,
      `  • Copiate con successo: ${color.green(subResult.migrated)}`,
      `  • Saltate (già presenti): ${color.yellow(subResult.skipped)}`,
      `  • Errori: ${subResult.failed > 0 ? color.red(subResult.failed) : color.green(0)}`,
      ``,
      `${color.bold('Playlist:')}`,
      `  • Copiate con successo: ${color.green(plResult.migrated)}`,
      `  • Saltate: ${color.yellow(plResult.skipped)}`,
      `  • Errori: ${plResult.failed > 0 ? color.red(plResult.failed) : color.green(0)}`
    ].join('\n'),
    'Report Finale'
  );

  if (subResult.failed === 0 && plResult.failed === 0) {
    storage.clearCheckpoint();
    p.outro(color.bold(color.green('🎉 Trasferimento completato con successo!')));
  } else {
    p.outro(
      color.yellow(
        '⚠️ Trasferimento completato con alcuni errori. Il checkpoint è stato salvato: puoi rieseguire con --resume per riprovare i falliti.'
      )
    );
  }
}

main().catch((err) => {
  p.log.error(color.red(`Errore fatale: ${err?.message || err}`));
  process.exit(1);
});
