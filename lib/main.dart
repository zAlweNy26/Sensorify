import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:sensorify/base_sensor.dart';
import 'package:sensorify/home.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp.router(
      routerConfig: GoRouter(
        routes: <RouteBase>[
          GoRoute(
            path: '/',
            builder: (BuildContext context, GoRouterState state) {
              return const HomeScreen();
            },
            routes: <RouteBase>[
              GoRoute(
                path: 'details',
                builder: (BuildContext context, GoRouterState state) {
                  return const BaseSensor();
                },
              ),
            ],
          ),
        ],
      ),
    );
  }
}