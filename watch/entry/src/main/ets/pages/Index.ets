// Index.ets
// LeakWatch — Watch app landing page (placeholder for v0.1.0-alpha.1)
// Task 3 will replace this with a live battery card driven by BatteryMonitor.

@Entry
@Component
struct Index {
  @State message: string = 'LeakWatch';

  build() {
    Column() {
      Text(this.message)
        .fontSize(36)
        .fontWeight(FontWeight.Bold)
        .margin({ bottom: 16 });

      Text('v0.1.0-alpha.1')
        .fontSize(14)
        .fontColor(Color.Gray)
        .margin({ bottom: 32 });

      Text('Battery-frugal monitor')
        .fontSize(14)
        .fontColor(Color.Gray);
    }
    .width('100%')
    .height('100%')
    .justifyContent(FlexAlign.Center)
    .alignItems(HorizontalAlign.Center);
  }
}
