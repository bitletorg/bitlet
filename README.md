# BitLet
### A tiny bittorrent library

BitLet is a simple Java implementation of the [BitTorrent](http://en.wikipedia.org/wiki/BitTorrent) protocol.

It is the library that powers [BitLet.org](http://bitlet.org) (a BitTorrent client that runs entirely in the browser plugin, as a Java applet).

## Trying out the BitLet library
You can build the project sources using Maven, and execute a sample client by calling:

    java -cp target/wetorrent-1.0-SNAPSHOT.jar org.bitlet.wetorrent.Sample $1

Where $1 is a .torrent file you have on your filesystem.

## Developing with BitLet
You can review this [annotated example](https://github.com/bitletorg/bitlet/wiki/Annotated-Example) for an overview on how to use the BitLet library.

## License
BitLet is distributed under the Apache license. See `src/main/resources/license.txt` for the full license terms.
