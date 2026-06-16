// BatteryRecord.ets
// Data model for a single battery reading.

export interface BatteryRecord {
  /** Unix timestamp in ms */
  ts: number;
  /** Battery state of charge, 0-100 */
  level: number;
  /** 0 = unknown, 1 = AC, 2 = USB, 3 = wireless */
  pluggedType: number;
  /** 0 = unknown, 1 = charging, 2 = not charging, 3 = full */
  chargingStatus: number;
  /** Voltage in microvolts */
  voltage: number;
  /** Temperature in 0.1°C */
  temperature: number;
  /** True if battery is present */
  isPresent: boolean;
}
