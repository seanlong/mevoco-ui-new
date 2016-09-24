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
                    client.subscribe("/user/queue/reply", function(message) {
                        var result = JSON.parse(message.body);
                        // Assume the first reply message is login reply
                        if (!received) {
                            received = true;
                            window.sessionUuid = result["org.zstack.header.identity.APILogInReply"].inventory.uuid;
                        } else {
                            var callId;
                            for (var k in result) {
                                if (result[k].session && result[k].session.callid)
                                    callId = result[k].session.callid;
                            }
                            if (!callId || !window.pendingCalls[callId]) {
                                console.error("message could not be handled:", result);
                            } else {
                                window.pendingCalls[callId](result);
                                delete window.pendingCalls[callId];
                            }
                        }
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

                function createCallId(length) {
                    var charset = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
                    var i;
                    var result = "api-";
                    values = new Uint32Array(length);
                    window.crypto.getRandomValues(values);
                    for(i=0; i<length; i++) {
                        result += charset[values[i] % charset.length];
                    }
                    return result;
                }

                window.pendingCalls = {};
                function apiCall(isSync, data) {
                    return new Promise(function(resolve) {
                        var callId = createCallId(8);
                        data[Object.keys(data)[0]].session = {
                            uuid: window.sessionUuid,
                            callid: callId
                        };
                        client.send(isSync ? "/app/sync" : "/app/async", {}, JSON.stringify(data));
                        window.pendingCalls[callId] = resolve;
                    });
                }

                function getImages() {
                    data = {
                        "org.zstack.header.image.APIQueryImageMsg": {
                          count: false,
                          start: 0,
                          limit: "20",
                          replyWithCount: true,
                          sortBy: "name",
                          sortDirection: "asc",
                          conditions: [ {"name":"status","op":"!=","value":"Deleted"} ],
                        }
                    };
                    return apiCall(true, data);
                }

                function setImageState(enable) {
                    var data = {
                        "org.zstack.header.image.APIChangeImageStateMsg": {
                            "uuid": "e52b3074fcb746129edf09cb97bf5ca7",
                            "stateEvent": enable ? "enable" : "disable",
                        }
                    };
                    return apiCall(false, data);
                }

                function createVm() {
                    var data = {
                        "org.zstack.header.vm.APICreateVmInstanceMsg": {
                            "name": "test",
                            "instanceOfferingUuid": "53202395b66641498d903323c0c47327",
                            "imageUuid": "b2daec3f1bac40fd89c4d830ba340238",
                            "l3NetworkUuids": ["9f4cec52952b4589be0ec5c5a2eaa3fb"],
                            "dataDiskOfferingUuids":[],
                            "defaultL3NetworkUuid":"9f4cec52952b4589be0ec5c5a2eaa3fb",
                            "systemTags":[],
                        }
                    };

                    return apiCall(false, data);
                }

                var imageEnabled = true;
                $("#toggleImage").click(function() {
                    getImages()
                        .then(function(ret) {
                            console.log(ret);
                            return setImageState(imageEnabled);
                        })
                        .then(function(ret) {
                            console.log(ret);
                            imageEnabled = !imageEnabled;
                        });
                });

                $("#createVm").click(function() {
                    createVm()
                        .then(function(ret) {
                            console.log(ret);
                        });
                });
            });
        </script>
    </head>
    <body>
        <button id="helloButton">hello</button>
        <button id="loginButton">login</button>
        <button id="toggleImage">toggle image state</button>
        <button id="createVm">create virtual machine</button>
        <div id="helloDiv"></div>
    </body>
</html>