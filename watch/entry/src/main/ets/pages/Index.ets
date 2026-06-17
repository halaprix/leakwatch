// Index.ets
// LeakWatch watch landing page — live battery card.

import common from '@ohos.app.ability.common';
import { BatteryMonitor } from '../service/BatteryMonitor';
import { BatteryRecord } from '../model/BatteryRecord';

@Entry
@Component
struct Index {
  @State level: number = 0;
  @State isCharging: boolean = false;
  @State temperature: number = 0;
  private monitor: BatteryMonitor = BatteryMonitor.getInstance();
  // Store the bound listener reference so aboutToDisappear can
  // deregister the same closure that aboutToAppear registered.
  // Without this, each navigation adds a fresh closure to the
  // monitor's Set that nothing removes — slow leak across pages.
  private listener: (record: BatteryRecord) => void = (_record) => {};

  aboutToAppear(): void {
    this.listener = (record: BatteryRecord) => {
      this.level = record.level;
      this.isCharging = record.chargingStatus === 1;
      this.temperature = record.temperature / 10; // 0.1°C → °C
    };
    this.monitor.addListener(this.listener);
    const last = this.monitor.getLastSample();
    if (last) {
      this.level = last.level;
      this.isCharging = last.chargingStatus === 1;
      this.temperature = last.temperature / 10;
    }
  }

  aboutToDisappear(): void {
    this.monitor.removeListener(this.listener);
  }

  build() {
    Column() {
      Text(this.isCharging ? '⚡' : '🔋')
        .fontSize(64)
        .margin({ bottom: 16 });

      Text(`${this.level}%`)
        .fontSize(48)
        .fontWeight(FontWeight.Bold)
        .margin({ bottom: 8 });

      Text(this.isCharging ? 'Charging' : 'On battery')
        .fontSize(14)
        .fontColor(Color.Gray)
        .margin({ bottom: 24 });

      Text(`${this.temperature.toFixed(1)}°C`)
        .fontSize(12)
        .fontColor(Color.Gray);
    }
    .width('100%')
    .height('100%')
    .justifyContent(FlexAlign.Center)
    .alignItems(HorizontalAlign.Center);
  }
}
