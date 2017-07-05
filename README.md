# Aix Weather Widget

Source code for the [Aix Weather Widget](https://play.google.com/store/apps/details?id=net.veierland.aix) Android app.

This app was written as a personal project in order to provide a compact graphical weather graph as a single row Android widget. The app met its original design goal at an early version, and most subsequent versions has been bug fixes. The source code is dated and not shiny, and there are several hacks used in order to make a graphical widget work on Android. The source code is made public domain as it may provide utility for others. Please respect the various APIs used by the app, and please modify the user agent if you are running a modified version of the app. I can only allocate a minimal amount of time to this app and will not spend further time developing it, but please email me via Google Play if you wish to become a maintainer or plan an improvement of the app.

# Acknowledgements

* The Norwegian Meteorological Institute for providing an [open weather data API](https://api.met.no/#english).
* The National Weather Service for providing an [open weather data API](https://graphical.weather.gov/xml/rest.php).
* The GeoNames database for providing their [timezone API](http://www.geonames.org/export/web-services.html#timezone).
* The [Google Maps Geocoding API](https://developers.google.com/maps/documentation/geocoding/intro).
* Thanks to [bharathp666 from DeviantArt](http://bharathp666.deviantart.com/) for the [application icon](http://bharathp666.deviantart.com/art/Android-Weather-Icons-180719113) (`app_icon.png`).

# License

* All code written as part of the app is licensed as [CC0 Universal](https://creativecommons.org/publicdomain/zero/1.0/). The only exceptions are `MultiKey.java` and `Pair.java` which are licensed under Apache 2.0 as specified in their headers.
* The weather icons are owned by The Norwegian Meteorological Institute and are as provided via their [weathericon API](http://api.met.no/weatherapi/weathericon/1.1/documentation).

# Information for use

* Any use of the provided software must respect the terms of each API used.
* [The user agent information must be changed in order to identify the modified application](https://github.com/pveierland/aix-weather-widget/blob/master/Aix/src/net/veierland/aix/AixUtils.java#L491).
* `Aix` and `Aixd` are twins with the first being packaged as `net.veierland.aix`, and the second as `net.veierland.aixd`. This was chosen as an early hack in order to be able to publish a separate donation version.
