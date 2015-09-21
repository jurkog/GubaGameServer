# GubaGameServer

A very simple TCP mixed UDP implementation of a server used for GubaGame. I did not implement any security / optimization simply because this was a test of getting a multiplayer game running. **I am totally aware things could be done better.**

## How it works (Briefly):

1. Client connects
2. Server redirects client to a new Thread (I know this is bad *now*)
3. Server listens on client thread for movement updates
4. Sends message for all connected clients to update the position of the client who just moved/fired/logged off.

### I stopped working on this because I found other projects I wanted to work on and I had school. No body was working on this with me either so I had no real inspiration to keep going.
