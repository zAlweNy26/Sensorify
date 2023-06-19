import 'package:flutter/material.dart';
import 'package:flutter/scheduler.dart';
import 'package:go_router/go_router.dart';
import 'package:sensorify/pigeon.dart';

class BatterySensor extends StatefulWidget {
  const BatterySensor({ Key? key }) : super(key: key);

  @override
  State<BatterySensor> createState() => _BatteryState();
}

class _BatteryState extends State<BatterySensor> {

  @override
  void initState() {
    super.initState();

    SchedulerBinding.instance.addPostFrameCallback((_) async {
        var battery = await SensorsApi().getBattery();
        var system = await SensorsApi().getSystemInfo();
        print('Battery level: ${battery.currentLevel}');
        print('Battery total capacity: ${battery.totalCapacity}');
        print('Battery technology: ${battery.technology}');
        print('Android version: ${system.androidVersion}');
        setState(() {});
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    return ElevatedButton(
      onPressed: () => context.go('/'),
      child: const Text('Go back to Home'),
    );
  }
}
