import 'package:flutter/material.dart';
import 'package:sensorify/sensors/battery.dart';

class BaseSensor extends StatelessWidget {
  const BaseSensor({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Details Screen')),
      body: const Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            BatterySensor()
          ],
        ),
      ),
    );
  }
}
