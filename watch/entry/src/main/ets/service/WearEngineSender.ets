// WearEngineSender.ets
// Stub. Real HMS Wear Engine P2P integration lands in v0.4.0-alpha.1
// after AGC fingerprint is provisioned. For now, logs the queue length
// every 10 minutes so we can verify the batching cadence is correct.

import hilog from '@ohos.hilog';

import { BatteryRecord } from '../model/BatteryRecord';
import { BatteryTable } from '../data/BatteryTable';

const DOMAIN = 0x0000;
const TAG = 'WearEngineSender';
const FLUSH_INTERVAL_MS = 10 * 60 * 1000; // 10 minutes

export class WearEngineSender {
  private static instance: WearEngineSender | null = null;
  private table: BatteryTable = new BatteryTable();
  private flushTimer: number = -1;
  private enabled: boolean = false; // flips to true in v0.4.0-alpha.1

  private constructor() {}

  static getInstance(): WearEngineSender {
    if (!WearEngineSender.instance) {
      WearEngineSender.instance = new WearEngineSender();
    }
    return WearEngineSender.instance;
  }

  start(): void {
    if (this.flushTimer !== -1) return;
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
    hilog.info(DOMAIN, TAG, 'WearEngineSender stopped');
  }

  private async flush(): Promise<void> {
    const records = await this.table.selectAll();
    if (records.length === 0) return;

    if (!this.enabled) {
      hilog.info(DOMAIN, TAG, 'P2P not enabled yet, would have sent %{public}d records', records.length);
      return;
    }

    // TODO v0.4.0-alpha.1: real HMS Wear Engine P2P send
    // const message = new P2pMessage(recordsToJson(records));
    // p2pClient.send(pairedDevice, message, callback);
  }
}
