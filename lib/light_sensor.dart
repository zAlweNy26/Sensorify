import 'dart:async';
import 'package:permission_handler/permission_handler.dart';
import 'package:flutter/material.dart';
import 'package:flutter/scheduler.dart';
import 'package:flutter/services.dart';

class ChecKingLightIntensity extends StatefulWidget {
  const ChecKingLightIntensity({Key? key}) : super(key: key);

  @override
  _ChecKingLightIntensityState createState() => _ChecKingLightIntensityState();
}

class _ChecKingLightIntensityState extends State<ChecKingLightIntensity> {
  ///platform channels declaration
  final eventChannel = const EventChannel('platform.testing/light_sensor');
  final methodChannel = const MethodChannel('platform.testing/method_calls');
  final messageChannel = const BasicMessageChannel<String>('platform.testing/sensor_support', StringCodec());

  var lightIntensity = 0.0;
  bool isSensorActive = false;
  bool isSensorSupported = false;
  StreamSubscription<dynamic>? streamSubscription;

  @override
  void initState() {
    super.initState();

    ///To make sure the widget is fully built and settled
    ///before using context and setstate method
    SchedulerBinding.instance.addPostFrameCallback(
      (_) async {
        ///Sending a String message to check whether the light sensor
        /// is supported by the device.
        /// We expect the native code to respond with a String thus we infer the type.
        /// and also check for potential nullability
        String? response =
            await messageChannel.send('is Light Sensor supported?');
        if (response != null && response == 'true') {
          isSensorSupported = true;
          checkLightIntensity();
        } else {
          isSensorSupported = false;
        }
        setState(() {});
      },
    );
  }

  @override
  void dispose() {
    super.dispose();
    streamSubscription?.cancel();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Platform Channels'),
      ),
      body: Center(
          child: isSensorSupported
              ? Column(
                  mainAxisSize: MainAxisSize.max,
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Padding(
                      padding: const EdgeInsets.all(20.0),
                      child: Text(
                        'The Lighting Intensity is at\n$lightIntensity Lux',
                        style: Theme.of(context).textTheme.titleMedium,
                      ),
                    ),
                    OutlinedButton(
                        onPressed: () {
                          activateDeactivateSensor();
                        },
                        child: Text(
                            isSensorActive ? 'Stop Sensor' : 'Restart Sensor'))
                  ],
                )
              : const Center(child: Text('Light Sensor is not supported'))),
    );
  }

  Future<void> checkLightIntensity() async {
    try {
      ///Check if sensors permission is granted
      ///and request for it if not
      if (!await Permission.sensors.request().isGranted) {
        ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Please Allow Sensor Permissions')));
        return;
      }

      ///Receive and listen to an event stream being broadcasted from the native code.
      ///on the events channel
      ///Assigning it to a stream subscription variable for better management.
      streamSubscription =
          eventChannel.receiveBroadcastStream().listen((event) {
        print('Lighting Intensity is at: $event');
        if (!mounted) return;
        setState(() {
          lightIntensity = event;
          isSensorActive = true;
        });
      }, onError: (error) {
        print(error);
      });
    } catch (e) {
      print(e);
    }
  }

  Future<void> activateDeactivateSensor() async {
    try {
      ///Call a method that maps to the passed name on the method channel
      ///Also passing the current status of the light sensor as an argument to be
      ///manipulated by the native code
      ///We expect the native method to respond with a Boolean which is decoded to bool in dart
      bool success = await methodChannel
          .invokeMethod('activateDeactivateSensor', [isSensorActive]);
      if (success) {
        print('toggling sensor is okayy');
        isSensorActive = !isSensorActive;
        if (isSensorActive) {
          checkLightIntensity();
        } else {
          lightIntensity = 0.0;
          streamSubscription?.cancel();
          streamSubscription = null;
        }
      } else {
        print('toggling sensor failed');
      }
      setState(() {});
    } catch (e) {
      print(e);
    }
  }
}