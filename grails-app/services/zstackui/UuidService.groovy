package zstackui

class UuidService {

    static scope = "singleton"

    def uuid = UUID.randomUUID().toString().replace("-", "")

}
