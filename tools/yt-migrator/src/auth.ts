import { Innertube, UniversalCache, ClientType } from 'youtubei.js';
import * as p from '@clack/prompts';
import color from 'picocolors';
import type { StorageManager } from './storage.js';
import type { AccountProfile } from './types.js';

export interface AuthenticatedClient {
  yt: Innertube;
  ytWriter: Innertube;
  profile: AccountProfile;
}

export async function authenticateAccount(
  role: 'source' | 'target',
  label: string,
  storage: StorageManager,
  forceReauth = false
): Promise<AuthenticatedClient> {
  const cachedCreds = forceReauth ? null : storage.loadCredentials(role);

  // ClientType.TV has full browsing access for FEchannels, FEplaylist_aggregation, and playlist contents
  const yt = await Innertube.create({
    cache: new UniversalCache(false),
    client_type: ClientType.TV
  });

  // ClientType.TV_EMBEDDED has write access for playlist/create and playlist/addVideos
  const ytWriter = await Innertube.create({
    cache: new UniversalCache(false),
    client_type: ClientType.TV_EMBEDDED
  });

  let authResolve: (() => void) | null = null;
  const authPromise = new Promise<void>((resolve) => {
    authResolve = resolve;
  });

  yt.session.on('auth-pending', (data) => {
    p.note(
      [
        `${color.bold('1.')} Apri nel browser: ${color.cyan(color.underline(data.verification_url))}`,
        `${color.bold('2.')} Inserisci questo codice: ${color.bold(color.yellow(data.user_code))}`,
        `${color.bold('3.')} Seleziona l'account: ${color.bold(color.magenta(label))}`
      ].join('\n'),
      `Accedi per: ${label}`
    );
  });

  yt.session.on('auth', async ({ credentials }) => {
    storage.saveCredentials(role, credentials);
    await ytWriter.session.signIn(credentials);
    if (authResolve) authResolve();
  });

  yt.session.on('update-credentials', async ({ credentials }) => {
    storage.saveCredentials(role, credentials);
    try {
      await ytWriter.session.signIn(credentials);
    } catch {
      // Ignore
    }
  });

  if (cachedCreds) {
    const spinner = p.spinner();
    spinner.start(`Verifica sessione salvata per ${label}...`);
    try {
      await yt.session.signIn(cachedCreds);
      await ytWriter.session.signIn(cachedCreds);
      spinner.stop(color.green(`✓ Sessione ripristinata per ${label}`));
    } catch {
      spinner.stop(color.yellow(`! Sessione scaduta per ${label}, nuovo accesso richiesto.`));
      storage.clearCredentials(role);
      await yt.session.signIn();
      await authPromise;
    }
  } else {
    p.log.info(`Avvio accesso per ${color.bold(label)}...`);
    await yt.session.signIn();
    await authPromise;
  }

  // Extract account profile details
  const profile: AccountProfile = {
    name: label,
    hasChannel: true
  };

  try {
    const info = await yt.account.getInfo();
    if (info) {
      const anyInfo = info as any;
      const selectedItem =
        anyInfo?.contents?.contents?.find?.((c: any) => c?.is_selected) ||
        anyInfo?.contents?.contents?.[0];

      const accountName =
        selectedItem?.account_name?.text ||
        selectedItem?.account_name?.toString?.() ||
        anyInfo?.account_name?.toString?.();

      const accountEmail =
        selectedItem?.account_byline?.text ||
        anyInfo?.account_email?.toString?.();

      if (accountName) profile.name = accountName;
      if (accountEmail) profile.email = accountEmail;
      if (typeof selectedItem?.has_channel === 'boolean') {
        profile.hasChannel = selectedItem.has_channel;
      }
    }
  } catch {
    // If getInfo fails, fallback to default
  }

  return { yt, ytWriter, profile };
}

export async function ensureTargetChannelExists(client: AuthenticatedClient): Promise<void> {
  // If account already has a channel, nothing to do
  if (client.profile.hasChannel) return;

  p.note(
    [
      color.bold(color.yellow('Per creare e copiare playlist, YouTube richiede che l\'Account B abbia un Canale associato.')),
      '',
      `${color.bold('1.')} Apri nel browser: ${color.cyan(color.underline('https://www.youtube.com/create_channel'))}`,
      `${color.bold('2.')} Assicurati di essere collegato con ${color.magenta(client.profile.email || client.profile.name || 'Account B')}`,
      `${color.bold('3.')} Clicca su ${color.green('"Crea canale"')}`,
      '',
      color.dim('(È un\'operazione immediata di 5 secondi che sblocca i permessi per le playlist)')
    ].join('\n'),
    'Azione richiesta per Account B'
  );

  let verified = false;
  while (!verified) {
    const confirmed = await p.confirm({
      message: 'Hai completato la creazione del canale per l\'Account B?',
      initialValue: true
    });

    if (p.isCancel(confirmed) || !confirmed) {
      p.log.warn('La creazione delle playlist potrebbe fallire senza un canale attivo.');
      break;
    }

    const spinner = p.spinner();
    spinner.start('Verifica canale su Account B...');
    try {
      const info = await client.yt.account.getInfo();
      const anyInfo = info as any;
      const selectedItem =
        anyInfo?.contents?.contents?.find?.((c: any) => c?.is_selected) ||
        anyInfo?.contents?.contents?.[0];

      if (selectedItem?.has_channel) {
        client.profile.hasChannel = true;
        verified = true;
        spinner.stop(color.green('✓ Canale verificato per Account B! Permessi playlist abilitati.'));
      } else {
        spinner.stop(color.yellow('! Canale non ancora rilevato. Assicurati di aver cliccato "Crea canale" nel browser.'));
      }
    } catch {
      spinner.stop(color.yellow('! Impossibile verificare automaticamente, procedo.'));
      break;
    }
  }
}
