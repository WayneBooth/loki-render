# Loki Render

Loki Render allows you to create your own render farm, serving Blender render jobs to a group of computers. Loki is easy to setup and runs on Linux, Windows, or Mac, making it a quick and flexible distributed network rendering solution!

This is a fork of the original project by Daniel Petersen over on [Sourceforge](https://sourceforge.net/projects/loki-render/) 

## Getting Started

First make sure you have java installed on your computer. I test with OpenJDK 8, but Oracle's JDK 8 should also work.
Then, of course, you will want to grab the latest JAR file from the [release page](https://github.com/mikeperalta1/loki-render/releases).

After this, Loki Render can be launched with a simple command:

```
java -jar LokiRender-<version>.jar [<BlenderExe>] [MasterIP]
```

* Replace <version> to match the jar file you have
* Add the full path to the Loki Render jar file if you wish to run the command outside the directory where the jar is stored
* Use [\<BlenderExe\>] by adding the path to a Blender executable, to start Loki Render in grunt mode without the GUI (great for headless rendering)
* Use [\<MasterIP\>] to specify the IP address of a server running Loki Render in Master mode, otherwise Loki Render will attempt to auto detect

Examples:
```
java -jar LokiRender-071-1.jar
java -jar LokiRender-071-1.jar /path/to/blender
java -jar LokiRender-071-1.jar 192.168.17.45
java -jar LokiRender-071-1.jar /path/to/blender 192.168.17.45
java -jar /path/to/jar/folder/LokiRender-071-1.jar /path/to/blender 192.168.17.45
```

### Prerequisites

You should have either Oracle's JDK 8 or OpenJDK 8 installed. This project is mostly tested on Linux, but should also work on Windows and Mac as well.

### Installing

Simply copy the jar file somewhere, and run the command above.

### Further Information

More in depth information will eventually be made available in the [Wiki](https://github.com/mikeperalta1/loki-render/wiki) section.

### And coding style tests

TO-DO

## Built With

* Netbeans IDE 8.1

## Contributing

* Please feel free to send bug fixes and improvements for consideration. I may not be able to put much time into this project, so it needs support from the community.
* If you can't contribute code, but would still like to help, please consider contributing to the [Wiki](https://github.com/mikeperalta1/loki-render/wiki).
I would like the goal of the Wiki to become an easy guide even for those who are not technically inclined.

## Versioning

We use [SemVer](http://semver.org/) for versioning. For the versions available, see the [tags on this repository](https://github.com/mikeperalta1/loki-render/tags). 

## Authors

* **Daniel Petersen** - *Initial work* - [Original Project](https://sourceforge.net/projects/loki-render/)
* **Mike Peralta** - *Reviving bug fix and fork to this repo* - [Music](http://MikePeralta.com/)

See also the list of [contributors](https://github.com/mikeperalta1/loki-render/contributors) who participated in this project.

## License

This project is licensed under the [GPL v3 License](https://www.gnu.org/licenses/gpl-3.0.en.html) - see the [LICENSE.md](LICENSE.md) file for details

## Acknowledgments

* Huge thanks to Daniel Petersen for creating this wonderful and fun project in the first place, creating his initial wiki with useful starter information, and giving his blessing for this fix+fork to happen
* The wonderful people working on Blender
* [PurpleBooth](https://gist.github.com/PurpleBooth/) for this README [template](https://gist.github.com/PurpleBooth/109311bb0361f32d87a2)
* My dog



