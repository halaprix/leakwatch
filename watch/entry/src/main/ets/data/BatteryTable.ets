// BatteryTable.ets
// RDB schema for BatteryRecord. 24h rolling window.

import relationalStore from '@ohos.data.relationalStore';
import { BatteryRecord } from '../model/BatteryRecord';

const DB_NAME = 'leakwatch.db';
const TABLE = 'battery_readings';
const DB_VERSION = 1;

const SQL_CREATE_TABLE = `
  CREATE TABLE IF NOT EXISTS ${TABLE} (
    ts INTEGER PRIMARY KEY,
    level INTEGER NOT NULL,
    plugged_type INTEGER NOT NULL,
    charging_status INTEGER NOT NULL,
    voltage INTEGER NOT NULL,
    temperature INTEGER NOT NULL,
    is_present INTEGER NOT NULL
  )
`;

const SQL_DROP_OLDER_THAN_24H = `
  DELETE FROM ${TABLE} WHERE ts < ?
`;

const SQL_INSERT = `
  INSERT OR REPLACE INTO ${TABLE}
    (ts, level, plugged_type, charging_status, voltage, temperature, is_present)
  VALUES (?, ?, ?, ?, ?, ?, ?)
`;

const SQL_SELECT_ALL = `
  SELECT ts, level, plugged_type, charging_status, voltage, temperature, is_present
  FROM ${TABLE}
  ORDER BY ts ASC
`;

const TWENTY_FOUR_HOURS_MS = 24 * 60 * 60 * 1000;

export class BatteryTable {
  private rdbStore: relationalStore.RdbStore | null = null;

  async init(context: Context): Promise<void> {
    const config: relationalStore.StoreConfig = {
      name: DB_NAME,
      securityLevel: relationalStore.SecurityLevel.S1
    };
    this.rdbStore = await relationalStore.getRdbStore(context, config);
    await this.rdbStore.executeSql(SQL_CREATE_TABLE);
  }

  async insert(record: BatteryRecord): Promise<void> {
    if (!this.rdbStore) throw new Error('BatteryTable not initialised');
    await this.rdbStore.executeSql(SQL_INSERT, [
      record.ts,
      record.level,
      record.pluggedType,
      record.chargingStatus,
      record.voltage,
      record.temperature,
      record.isPresent ? 1 : 0
    ]);
  }

  async prune(): Promise<void> {
    if (!this.rdbStore) throw new Error('BatteryTable not initialised');
    const cutoff = Date.now() - TWENTY_FOUR_HOURS_MS;
    await this.rdbStore.executeSql(SQL_DROP_OLDER_THAN_24H, [cutoff]);
  }

  async selectAll(): Promise<BatteryRecord[]> {
    if (!this.rdbStore) throw new Error('BatteryTable not initialised');
    const result = await this.rdbStore.querySql(SQL_SELECT_ALL);
    const rows: BatteryRecord[] = [];
    for (let i = 0; i < result.rowCount; i++) {
      result.goToRow(i);
      rows.push({
        ts: result.getLong(result.getColumnIndex('ts')),
        level: result.getLong(result.getColumnIndex('level')),
        pluggedType: result.getLong(result.getColumnIndex('plugged_type')),
        chargingStatus: result.getLong(result.getColumnIndex('charging_status')),
        voltage: result.getLong(result.getColumnIndex('voltage')),
        temperature: result.getLong(result.getColumnIndex('temperature')),
        isPresent: result.getLong(result.getColumnIndex('is_present')) === 1
      });
    }
    return rows;
  }
}
