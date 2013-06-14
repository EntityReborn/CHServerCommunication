##Simple publish

Server A's `main.ms`:
	
    # Listen on port 5556 on all interfaces, using a publisher.
    comm_listen('example', 'tcp://*:5556')

Server A's `config.txt`:

    *:/publish1 $ = comm_publish('example', 'Channel1', $)
    *:/publish2 $ = comm_publish('example', 'Channel2', $)

Server B's `main.ms`:

    # Connect to our publisher, using a subscriber.
    comm_connect('example', 'tcp://localhost:5556')

    # Subscribe to a single channel.
    comm_subscribe('example', 'Channel1')

    bind('comm_received', null, null, @event,
        console(@event)
    )

If one runs `/publish1 Testing!` on Server A, Server B will print the following 
to it's console:

    {channel: Channel1, event_type: comm_received, publisherid: example, macrotype: custom, message: Testing!}

Calling `/publish2 Testing!` on Server A will have no affect on Server B because 
B is not listening to `Channel2`.

##Reverse connection

In the above example, Server A made it's publisher socket listen, and Server B's 
subscriber connected to that publisher. Some network topographies, such as if A 
were behind a router without forwarding, won't allow this. In this case, we can 
tell B to listen, and A to connect. Publishes will still travel from A to B, 
regardless.

Server A's `main.ms`:

    comm_connect('example', 'tcp://*:5556', 'PUB')
    ...

Server B's `main.ms`:

    comm_listen('example', 'tcp://localhost:5556', 'SUB')
    ...

##Proxy (Hub) mode

In the case that we want to use a central server to manage messages and share a 
single message between multiple subscribers, we can use a proxy pattern. This 
helps make network topography much more liquid, and can ease the problem of 
dynamic discovery.
	
    #====================== Server 1 (Publisher) =====================

    # Main.ms
    comm_connect('Publisher', 'tcp://localhost:5557', 'PUB')

    # Config.txt
    *:/publish $ = comm_publish('Publisher', 'Global', $)
    *:/chat $ = comm_publish('Publisher', 'Chat', $)

    # ===================== SERVER 2 (Proxy hub) =======================
    # Listen on port 5557 on all interfaces, using a subscriber.
    comm_listen('Proxysub', 'tcp://*:5557', 'SUB')

    # Subscribe to all channels.
    comm_subscribe('Proxysub', '*')

    # Forward all messages from SUB to PUB. Only listen to communications
    # from Proxysub.
    bind('comm_received', null, array('subscriberid': 'Proxysub'), @event,
        @pubid = @event['publisherid']
        @channel = @event['channel']
        @message = @event['message']

        # Use special _publish function to specify the ID being sent.
        comm_publish('Proxypub', @channel, @message, @pubid)
    )

    # Listen on port 5556 on all interfaces, using a publisher.
    comm_listen('Proxypub', 'tcp://*:5556')

    # ==================== Server 3 (Subscriber) =========================
    # Connect our sub to the proxy
    comm_connect('Somesub', 'tcp://localhost:5556')
    comm_subscribe('Somesub', '*')

    bind('comm_received', null, array('subscriberid': 'Somesub'), @event,
        console(@event)
    )