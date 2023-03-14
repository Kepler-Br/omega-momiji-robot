rootProject.name = "omega-momiji-bot"

include("api")
include("server")

findProject(":api")?.name = "${rootProject.name}-api"
