## Simple publish

Server A's `main.ms`:
	
    # Listen on port 5556 on all interfaces, using a publisher.
    comm_listen('example', 'tcp://*:5556')

Server A's `config.txt`:

    *:/publish1 $ = comm_publish('example', 'Channel1', $)

Server B's `main.ms`:

    # Connect to our publisher, using a subscriber.
    comm_connect('example', 'tcp://localhost:5556')

    bind('comm_received', null, null, @event,
        console(@event)
    )

If one runs `/publish1 Testing!` on Server A, Server B will print the following 
to it's console:

    {channel: Channel1, event_type: comm_received, publisherid: example, macrotype: custom, message: Testing!}

## Reverse connection

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

## Proxy (Hub) mode

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

    bind('comm_received', null, array('subscriberid': 'Somesub'), @event,
        console(@event)
    )

## Pseudo two-way communication

Currently, the framework sockets are individually one way. A PUB socket can only 
send to a SUB socket. But we can "abuse" this to create a pseudo two-way communication 
system: (This code is untested but in theory should work. YMMV)

    # ========================Server A========================

    # Config.txt
    *:/getplayers = >>>
        @req = array()
        @req['command'] = 'all_players()'
        @req['type'] = 'request'
        @req['source'] = player()

        comm_publish('ServerA', 'Channel', json_encode(@req))
        # The results will appear in the bind below.
    <<<

    # Main.ms
    comm_listen('ServerA', 'tcp://*:5556') # PUB
    comm_connect('FromServerB', 'tcp://a.myserver.com:5556') # SUB

    @filter = array()
    @filter['subscriberid'] = 'FromServerB'
    @filter['publisherid'] = 'ServerB'
    @filter['channel'] = 'Channel'

    bind('comm_received', null, @filter, @event,
        @msg = json_decode(@event['message'])

        if (@msg['type'] == 'response') {
            if (@msg['command'] == 'all_players()') {
                @retn = 'Players:' @msg['result']
                
                if (@msg['source'] == '~console') {
                    console(@retn)
                } else {
                    tmsg(@msg['source'], @retn)
                }
            }
        } else if (@msg['type'] == 'error') {
            @retn = color('red') . 'Error:' @msg['result']
            
            if (@msg['source'] == '~console') {
                console(@retn)
            } else {
                tmsg(@msg['source'], @retn)
            }
        }
    )

    # ========================Server B========================

    # Main.ms
    comm_listen('ServerB', 'tcp://*:5556') # PUB
    comm_connect('FromServerA', 'tcp://b.myserver.com:5556') # SUB

    @filter = array()
    @filter['subscriberid'] = 'FromServerA'
    @filter['publisherid'] = 'ServerA'
    @filter['channel'] = 'Channel'

    bind('comm_received', null, @filter, @event,
        @msg = json_decode(@event['message'])

        if (@msg['type'] == 'request') {
            if (@msg['command'] == 'all_players()') {
                @msg['type'] = 'response'
                @msg['result'] = all_players()

                comm_publish('ServerB', 'Channel', json_encode(@msg))

                return()
            }

            @msg['type'] = 'error'
            @msg['result'] = 'Unknown command:' @msg['command']
            
            comm_publish('ServerB', 'Channel', json_encode(@msg))
        }

        # ...
    )

On Server A, calling /getplayers should return an array of players on Server B, 
either sending the result to console or the player who called that alias.
