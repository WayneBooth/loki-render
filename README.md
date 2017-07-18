# Loki Render

Loki Render allows you to create your own render farm, serving Blender render jobs to a group of computers. Loki is easy to setup and runs on Linux, Windows or Mac, making it a quick and flexible distributed network rendering solution!

This is a fork of the original project by Daniel Petersen over on Sourceforge: https://sourceforge.net/projects/loki-render/

## Getting Started

Loki Render can be launched with a simple command line:

java -jar LokiRender-<version>.jar [<BlenderExe>] [MasterIP]

Replace <version> to match the jar file you have
Use [<BlenderExe>] by adding the path to a Blender executable, to start Loki Render in grunt mode
Use [<MasterIP>] to specify the IP address of a server running Loki Render in Master mode, otherwise Loki Render will attempt to auto detect


