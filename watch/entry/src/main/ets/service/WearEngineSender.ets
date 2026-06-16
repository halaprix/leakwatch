// WearEngineSender.ets
// Real HMS Wear Engine P2P integration for v0.4.0-alpha.1
// Sends battery readings from watch to phone via Wear Engine DataClient.

import hilog from '@ohos.hilog';
import { wearableEngine } from '@hw-wearable/wearable-engine';

import { BatteryRecord } from '../model/BatteryRecord';
import { BatteryTable } from '../data/BatteryTable';

const DOMAIN = 0x0000;
const TAG = 'WearEngineSender';
const FLUSH_INTERVAL_MS = 10 * 60 * 1000; // 10 minutes
const PATH_BATTERY_DATA = '/leakwatch/battery';
const KEY_BATTERY_READINGS = 'battery_readings';

export class WearEngineSender {
  private static instance: WearEngineSender | null = null;
  private table: BatteryTable = new BatteryTable();
  private flushTimer: number = -1;
  private dataClient: wearableEngine.DataClient | null = null;

  private constructor() {}

  static getInstance(): WearEngineSender {
    if (!WearEngineSender.instance) {
      WearEngineSender.instance = new WearEngineSender();
    }
    return WearEngineSender.instance;
  }

  /**
   * Initialize Wear Engine DataClient and start periodic flush.
   * Must be called after AGC setup is complete.
   */
  async start(): Promise<void> {
    if (this.flushTimer !== -1) {
      hilog.warn(DOMAIN, TAG, 'Already started');
      return;
    }

    try {
      // Initialize Wear Engine DataClient
      this.dataClient = wearableEngine.getDataClient();
      hilog.info(DOMAIN, TAG, 'WearEngine DataClient initialized');
    } catch (e) {
      hilog.error(DOMAIN, TAG, 'Failed to initialize DataClient: %{public}s', JSON.stringify(e));
      return;
    }

    // Start periodic flush (every 10 minutes)
    this.flushTimer = setInterval(() => {
      this.flush().catch((e) =>
        hilog.error(DOMAIN, TAG, 'flush failed: %{public}s', JSON.stringify(e))
      );
    }, FLUSH_INTERVAL_MS);

    hilog.info(DOMAIN, TAG, 'WearEngineSender started, flush every %{public}dms', FLUSH_INTERVAL_MS);
  }

  stop(): void {
    if (this.flushTimer !== -1) {
      clearInterval(this.flushTimer);
      this.flushTimer = -1;
    }
    this.dataClient = null;
    hilog.info(DOMAIN, TAG, 'WearEngineSender stopped');
  }

  /**
   * Flush pending battery records to phone via P2P.
   * Reads all records from local RDB, serializes to JSON, sends via DataClient.
   */
  private async flush(): Promise<void> {
    if (!this.dataClient) {
      hilog.warn(DOMAIN, TAG, 'DataClient not initialized, skipping flush');
      return;
    }

    const records = await this.table.selectAll();
    if (records.length === 0) {
      hilog.debug(DOMAIN, TAG, 'No records to flush');
      return;
    }

    try {
      // Serialize records to JSON
      const jsonData = this.recordsToJson(records);
      const rawData = new TextEncoder().encode(jsonData);

      // Create DataItem
      const dataItem: wearableEngine.DataItem = {
        path: PATH_BATTERY_DATA,
        data: rawData,
        isUrgent: false // Non-urgent, battery-frugal
      };

      // Send via P2P
      await this.dataClient.putDataItem(dataItem);
      
      hilog.info(DOMAIN, TAG, 'Sent %{public}d records to phone via P2P', records.length);

      // Clear sent records from local DB
      await this.table.deleteAll();
      hilog.debug(DOMAIN, TAG, 'Cleared %{public}d records from local DB', records.length);

    } catch (e) {
      hilog.error(DOMAIN, TAG, 'P2P send failed: %{public}s', JSON.stringify(e));
      // Don't delete records — retry on next flush
    }
  }

  /**
   * Serialize BatteryRecord array to JSON string.
   * Format matches phone-side BatteryReadingSerializer.
   */
  private recordsToJson(records: BatteryRecord[]): string {
    const serialized = records.map(r => ({
      ts: r.ts,
      level: r.level,
      pluggedType: r.pluggedType,
      chargingStatus: r.chargingStatus,
      voltage: r.voltage,
      temperature: r.temperature,
      isPresent: r.isPresent
    }));
    return JSON.stringify(serialized);
  }

  /**
   * Force immediate flush (for testing or low-battery scenarios).
   */
  async flushNow(): Promise<void> {
    await this.flush();
  }
}
