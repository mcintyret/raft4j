An implementation of the Raft consensus algorithm in Java

This is a relatively simple demo of Raft, and doesn't implement the more advanced features
(snapshotting and dealing with configuration changes - i.e dynamically adding and removing peers).

Also, it currently works will all peers in the same JVM communicating via a simple registry. However, it would be
 relatively straightforward to update the current implementation to work across the network.

To give it a go:
1. clone the repo and set up a project
2. run com.mcintyret.raft.Runner
3. this will create a folder called logs/, with a number of log files - one for each peer showing the committed state machine
of that peer, and one showing the log output of the consensus algorithm as it works. Tail -f those logs in separate windows
if you wish
4. interact with the peers by sending them messages. The possible messages are:

send: <id>: <message>
- send a message with text <message> to peer with id <id>

start: <id>
- start peer with id <id> (note all peers are initially started by default; this will error unless stop is called first)

stop
- stop peer with id <id>

mute: <id>
- prevent peer with id <id> from receiving any messages

unmute: <id>
- allow muted peer with id <id> to receive messages again