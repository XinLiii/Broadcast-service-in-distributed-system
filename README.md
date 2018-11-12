# AdvancedOS-project2

## Description

In this project, we design and implement a broadcast service in a distributed system.

Assume a distributed system in which nodes are arranged in a certain topology (specified in a configuration file). 
We build a spanning tree using a distributed algorithm. 
Once the spanning tree construction completes, each node should prints its tree neighbors.

Use the spanning tree constructed above, we implement a broadcast service that allows any node to send a message to all nodes in the system. 
The broadcast service will eventually inform the source node of the completion of the broadcast operation. 
We allow multiple broadcast operations in progress concurrently. 
And, we assume that two concurrent broadcast operations have distinct source nodes.

<b>Output</b>: Each node should print its set of tree neighbors. 
Each node should also output any broadcast message it sends or receives.

## Configuration Example
    
    # number of nodes
    5
    # nodeID, host address, port
    0 dc02.utdallas.edu 1234
    1 dc03.utdallas.edu 1233
    2 dc04.utdallas.edu 1233
    3 dc05.utdallas.edu 1232
    4 dc06.utdallas.edu 1233
    # nodeID, IDs of the direct neighbors
    0 1 2 4
    1 0 2 3
    2 0 1 3
    3 1 2
    4 0
