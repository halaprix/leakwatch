// BatteryMonitor.ets
// Singleton that samples @ohos.batteryInfo at 120s intervals,
// persists to BatteryTable, and exposes a listener API for UI.
//
// Hard-mode rules (see docs/ARCHITECTURE.md §3):
//   - 120s polling interval (not 60s)
//   - listener-driven events from batteryInfo.on('batteryInfoChange')
//     fire immediately, but flush through the same 120s debounce
//   - 24h rolling RDB history; older rows pruned on every insert
//   - No foreground service — lives on the UIAbility lifecycle only
//   - destroy() MUST be called from EntryAbility.onDestroy() to release the listener

import batteryInfo from '@ohos.batteryInfo';
import common from '@ohos.app.ability.common';
import hilog from '@ohos.hilog';

import { BatteryRecord } from '../model/BatteryRecord';
import { BatteryTable } from '../data/BatteryTable';
import { WearEngineSender } from './WearEngineSender';

const DOMAIN = 0x0000;
const TAG = 'BatteryMonitor';
const POLL_INTERVAL_MS = 120_000; // 120 seconds, hard mode

type BatteryListener = (record: BatteryRecord) => void;

export class BatteryMonitor {
  private static instance: BatteryMonitor | null = null;
  private table: BatteryTable = new BatteryTable();
  private listeners: Set<BatteryListener> = new Set();
  private pollTimer: number = -1;
  private nativeListener: batteryInfo.BatteryInfoCallback | null = null;
  private context: common.UIAbilityContext | null = null;
  private lastSample: BatteryRecord | null = null;

  private constructor() {}

  static getInstance(): BatteryMonitor {
    if (!BatteryMonitor.instance) {
      BatteryMonitor.instance = new BatteryMonitor();
    }
    return BatteryMonitor.instance;
  }

  async init(context: common.UIAbilityContext): Promise<void> {
    this.context = context;
    await this.table.init(context);
    await this.sampleOnce(); // immediate first sample
    this.startPolling();
    this.registerNativeListener();
    WearEngineSender.getInstance().start();
    hilog.info(DOMAIN, TAG, 'BatteryMonitor initialised, polling at %{public}dms', POLL_INTERVAL_MS);
  }

  destroy(): void {
    WearEngineSender.getInstance().stop();
    if (this.pollTimer !== -1) {
      clearInterval(this.pollTimer);
      this.pollTimer = -1;
    }
    if (this.nativeListener) {
      try {
        batteryInfo.off('batteryInfoChange', this.nativeListener);
      } catch (e) {
        hilog.warn(DOMAIN, TAG, 'Failed to unregister native listener: %{public}s', JSON.stringify(e));
      }
      this.nativeListener = null;
    }
    this.listeners.clear();
    hilog.info(DOMAIN, TAG, 'BatteryMonitor destroyed');
  }

  addListener(listener: BatteryListener): void {
    this.listeners.add(listener);
  }

  removeListener(listener: BatteryListener): void {
    this.listeners.delete(listener);
  }

  getLastSample(): BatteryRecord | null {
    return this.lastSample;
  }

  private startPolling(): void {
    this.pollTimer = setInterval(() => {
      this.sampleOnce();
    }, POLL_INTERVAL_MS);
  }

  private registerNativeListener(): void {
    this.nativeListener = {
      onBatteryInfoChanged: (info: batteryInfo.BatteryInfo) => {
        // Native event — record and persist immediately, but the 120s timer
        // also calls sampleOnce() so we don't double-count. Use this path
        // for low-latency UI updates only.
        const record = this.fromBatteryInfo(info);
        this.lastSample = record;
        this.notifyListeners(record);
      }
    };
    batteryInfo.on('batteryInfoChange', this.nativeListener);
  }

  private async sampleOnce(): Promise<void> {
    try {
      const info = batteryInfo.getBatteryInfo();
      const record = this.fromBatteryInfo(info);
      this.lastSample = record;
      await this.table.insert(record);
      await this.table.prune();
      this.notifyListeners(record);
    } catch (e) {
      hilog.error(DOMAIN, TAG, 'sampleOnce failed: %{public}s', JSON.stringify(e));
    }
  }

  private fromBatteryInfo(info: batteryInfo.BatteryInfo): BatteryRecord {
    return {
      ts: Date.now(),
      level: info.batterySOC,
      pluggedType: info.pluggedType,
      chargingStatus: info.chargingStatus,
      voltage: info.voltage,
      temperature: info.batteryTemperature,
      isPresent: info.isBatteryPresent ?? true
    };
  }

  private notifyListeners(record: BatteryRecord): void {
    this.listeners.forEach((l) => {
      try { l(record); } catch (e) {
        hilog.warn(DOMAIN, TAG, 'listener threw: %{public}s', JSON.stringify(e));
      }
    });
  }
}
