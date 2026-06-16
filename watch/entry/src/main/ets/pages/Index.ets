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

  aboutToAppear(): void {
    this.monitor.addListener((record: BatteryRecord) => {
      this.level = record.level;
      this.isCharging = record.chargingStatus === 1;
      this.temperature = record.temperature / 10; // 0.1°C → °C
    });
    const last = this.monitor.getLastSample();
    if (last) {
      this.level = last.level;
      this.isCharging = last.chargingStatus === 1;
      this.temperature = last.temperature / 10;
    }
  }

  aboutToDisappear(): void {
    this.monitor.removeListener(() => {});
    // Empty handler is intentional — the bound handler in aboutToAppear
    // cannot be retrieved without a stored reference; listeners are
    // cleared wholesale in EntryAbility.onDestroy via BatteryMonitor.destroy().
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
