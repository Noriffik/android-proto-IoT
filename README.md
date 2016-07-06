# Android Proto IoT

Welcome to Proto IoT, an open-source Android app for rapid Internet of
Things prototyping!

## Table of Contents

1.  [Introduction](#introduction)
2.  [Android & Java SDK reference](#android--java-sdk-reference)
3.  [Using the app](#using-proto-iot)  
  1.  [Setting it up on your device](#setting-it-up-on-your-device)
  2.  [Viewing device data](#viewing-device-data)
  3.  [How data is transferred](#how-data-is-transferred)
  4.  [Rules](#rules)
4.  [Developing the app](#developing-the-app)
5.  [Other resources](#other-resources)

## Introduction

Proto IoT turns your smartphone into a cloud-connected IoT device by measuring
sensor data from your device (_e.g._ touch, battery, wifi signal) and sending
it to the cloud over a secure MQTT connection. You can interact with your
device and view the data in the [relayr Developer
Dashboard](http://developer.relayr.io).

Using Proto IoT, you can:

-  Access sensor data easily and securely from anywhere with an internet connection.
-  Send readings from your device's sensors to the relayr cloud and interact with them remotely through the Dashboard.
-  Add interactions and rules on top of your sensors’ data in order to trigger actions on separate devices via the mobile app or the Dashboard.
-  Build and demonstrate IoT prototype solutions quickly, using just a smartphone.
-  View historical data from your devices.
-  Connect your wearable to the Internet of Things.
-  Trigger your device's actuators.

[**Download on Google Play**](https://play.google.com/store/apps/details?id=io.relayr.iotsmartphone&hl=en)

Proto IoT is open-source, so if you are a developer, take a look under the
hood or connect your own application through relayr’s RESTful API.

## Android & Java SDK reference

Proto IoT uses relayr's [Java](https://relayr.github.io/java-sdk/) and
[Android](https://github.com/relayr/android-sdk) SDKs in order to interact
with the relayr cloud.

## Using Proto IoT

### Setting it up on your device

Before you can connect your device to the cloud, you'll need a relayr
Developer Dashboard account. You can register one either [by visiting the
Dashboard](http://developer.relayr.io) or through the app as described below.

To connect your phone to the cloud:

1.  Open the Proto IoT app and click the cloud icon.
2.  In the first section labeled "Establish connection with the relayr Cloud," click **Log in**.
3.  On the login screen, if you don't have an account yet, switch to the **Create an account** tab, fill in the account details and click **Create account**. Otherwise, enter your account credentials and click **Log in.**

At this point, you're all set! Your device will begin pushing sensor data to the cloud.

### Viewing device data

You can see the data measured by your smartphone's sensors on the _smartphone
tab_, which is the first tab you see when you open the app. It's the leftmost
tab at the top, denoted by a chip icon. On this tab, you can see the following
data readings:

-  **Acceleration**: Measures the rate of your smartphone's acceleration among the x, y and z axes, in `m/s²`.
-  **Gyroscope**: Measures the angular tilt of your smartphone, in `m/s²`.
-  **Luminosity**: Measures the amount of light hitting your smartphone's camera, in `lux`.  
  **Note:** In order 
-  **Location**: Records your phone's physical location.  
  **Note:** The Proto IoT app requires permission to use your device's location. You can double-check to make sure this is enabled under Settings > Apps > relayr Proto IoT.
-  **Screen touch**: Records every instance when your phone's screen is touched.
-  **Battery**: Measures your phone's battery percentage.
-  **WiFi signal**: Measures the strength of your phone's WiFi signal relative to the wifi network, in `dBi`. The signal strength is measured on a scale of -120 (worst connection) to 0 (best connection).

### How data is transferred

Todo

### Rules

Todo

## Developing the app

Todo

## Other resources

[Introducing Proto IoT](http://blog.relayr.io/engineering/introducing-proto-iot-for-android)

[Example project](https://github.com/bernardpletikosa/droidcon-workshop-2016)

**YouTube: Introducing Proto IoT for Android**

[![Introducing Proto IoT for Android](video_tmb.png)](https://www.youtube.com/watch?v=s55vkryfQSY "Introducing Proto IoT for Android | relayr")