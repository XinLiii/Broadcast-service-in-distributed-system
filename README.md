# Broadcast service in a distributed system

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
    
   # Hostname   Port    Neighbor List
    dc01        3332    2 4 5
    dc33        5678    1 3
    dc21        5231    2 4 5
    dc33        2311    1 3 5
    dc22        3124    1 3 4
