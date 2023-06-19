import 'package:pigeon/pigeon.dart';

enum BatteryCharge {
  none, ac, usb, wireless
}

enum BatteryHealth {
  unknown, good, overHeat, overVoltage, cold, dead
}

enum BatteryState {
  unknown, notCharging, discharging, charging, full
}

class Battery {
  int currentLevel;
  int realCurrentLevel;
  int currentCapacity;
  int totalCapacity;
  int voltage;
  int amperage;
  double temperature;
  double wattage;
  String technology;
  BatteryCharge chargeMode;
  BatteryHealth health;
  BatteryState state;

  Battery(
    this.currentLevel,
    this.realCurrentLevel,
    this.currentCapacity,
    this.totalCapacity,
    this.technology,
    this.chargeMode,
    this.temperature,
    this.wattage,
    this.voltage,
    this.amperage,
    this.health,
    this.state
  );
}

class System {
  String androidVersion;
  int sdkVersion;

  System(
    this.androidVersion,
    this.sdkVersion
  );
}

@HostApi()
abstract class SensorsApi {
  Battery getBattery();
  System getSystemInfo();
}