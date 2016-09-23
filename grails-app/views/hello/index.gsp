<!DOCTYPE html>
<html>
    <head>
        <meta name="layout" content="main"/>

        <asset:javascript src="application" />
        <asset:javascript src="sockjs" />
        <asset:javascript src="stomp" />

        <script type="text/javascript">
            $(function() {
                var socket = new SockJS("${createLink(uri: '/stomp')}");
                var client = Stomp.over(socket);

                var header = { };
                client.connect(header, function() {
                    client.subscribe("/topic/hello", function(message) {
                        $("#helloDiv").append(message.body);
                    });
                    var received = false;
                    client.subscribe("/user/queue/hello", function(message) {
                        var result = JSON.parse(message.body);
                        // Assume the first reply message is login reply
                        if (!received) {
                            received = true;
                            window.sessionUuid = result["org.zstack.header.identity.APILogInReply"].inventory.uuid;
                        }
                        console.log(result);
                    });
                });

                $("#helloButton").click(function() {
                    client.send("/app/hello", {}, JSON.stringify("world"));
                });

                $("#loginButton").click(function() {
                    var data = {
                        "org.zstack.header.identity.APILogInByAccountMsg": {
                          accountName: "admin",
                          password: "b109f3bbbc244eb82441917ed06d618b9008dd09b3befd1b5e07394c706a8bb980b1d7785e5976ec049b46df5f1326af5a2ea6d103fd07c95385ffab0cacbc86", // sha512 hash of 'password'
                        }
                    };
                    client.send("/app/sync", {}, JSON.stringify(data));
                });

                var enabled = true;
                $("#toggleImage").click(function() {
                    var data = {
                        "org.zstack.header.image.APIChangeImageStateMsg": {
                            "uuid": "e52b3074fcb746129edf09cb97bf5ca7",
                            "stateEvent": enabled ? "disable" : "enable",
                            "session": { "uuid": window.sessionUuid }
                        }
                    };
                    enabled = !enabled;
                    client.send("/app/async", {}, JSON.stringify(data));

                    data = {
                        "org.zstack.header.image.APIQueryImageMsg": {
                          count: false,
                          start: 0,
                          limit: "20",
                          replyWithCount: true,
                          sortBy: "name",
                          sortDirection: "asc",
                          conditions: [
                            {"name":"status","op":"!=","value":"Deleted"}
                          ],
                          session: {
                            uuid: window.sessionUuid
                          }
                        }
                    };
                    client.send("/app/sync", {}, JSON.stringify(data));
                });
            });
        </script>
    </head>
    <body>
        <button id="helloButton">hello</button>
        <button id="loginButton">login</button>
        <button id="toggleImage">toggle</button>
        <div id="helloDiv"></div>
    </body>
</html>