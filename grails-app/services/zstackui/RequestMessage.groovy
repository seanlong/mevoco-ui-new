package zstackui

class RequestMessage {

    def String username
    def String message

    public RequestMessage(String username, String message) {
        this.username = username
        this.message = message
    }
}
