import fs from 'node:fs';
import path from 'node:path';
import os from 'node:os';
import type { MigrationCheckpoint } from './types.js';

const DEFAULT_BASE_DIR = path.join(os.homedir(), '.yt-migrator');

export class StorageManager {
  private baseDir: string;

  constructor(customDir?: string) {
    this.baseDir = customDir || DEFAULT_BASE_DIR;
    this.ensureDir(this.baseDir);
  }

  private ensureDir(dir: string): void {
    if (!fs.existsSync(dir)) {
      fs.mkdirSync(dir, { recursive: true });
    }
  }

  private getCredentialsPath(role: 'source' | 'target'): string {
    return path.join(this.baseDir, `${role}_credentials.json`);
  }

  private getCheckpointPath(): string {
    return path.join(this.baseDir, 'checkpoint.json');
  }

  public getBaseDir(): string {
    return this.baseDir;
  }

  public loadCredentials(role: 'source' | 'target'): any | null {
    const filePath = this.getCredentialsPath(role);
    if (!fs.existsSync(filePath)) return null;
    try {
      const data = fs.readFileSync(filePath, 'utf-8');
      return JSON.parse(data);
    } catch {
      return null;
    }
  }

  public saveCredentials(role: 'source' | 'target', credentials: any): void {
    const filePath = this.getCredentialsPath(role);
    fs.writeFileSync(filePath, JSON.stringify(credentials, null, 2), 'utf-8');
  }

  public clearCredentials(role?: 'source' | 'target'): void {
    if (role) {
      const filePath = this.getCredentialsPath(role);
      if (fs.existsSync(filePath)) fs.unlinkSync(filePath);
    } else {
      const sourcePath = this.getCredentialsPath('source');
      const targetPath = this.getCredentialsPath('target');
      if (fs.existsSync(sourcePath)) fs.unlinkSync(sourcePath);
      if (fs.existsSync(targetPath)) fs.unlinkSync(targetPath);
    }
  }

  public loadCheckpoint(): MigrationCheckpoint | null {
    const filePath = this.getCheckpointPath();
    if (!fs.existsSync(filePath)) return null;
    try {
      const data = fs.readFileSync(filePath, 'utf-8');
      return JSON.parse(data);
    } catch {
      return null;
    }
  }

  public saveCheckpoint(checkpoint: MigrationCheckpoint): void {
    const filePath = this.getCheckpointPath();
    checkpoint.updatedAt = new Date().toISOString();
    fs.writeFileSync(filePath, JSON.stringify(checkpoint, null, 2), 'utf-8');
  }

  public clearCheckpoint(): void {
    const filePath = this.getCheckpointPath();
    if (fs.existsSync(filePath)) {
      fs.unlinkSync(filePath);
    }
  }
}
