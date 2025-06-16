import android.R.attr.configure
import android.R.attr.name
import android.R.attr.value

// GitLab Maven publishing configuration
// Apply this to build.gradle.kts with: apply(from = "gitlab-publish.gradle.kts")

if (project.hasProperty("enableGitlabPublish")) {
    val gitlabProjectId = project.findProperty("gitlabProjectId") as String?
        ?: System.getenv("GITLAB_PROJECT_ID")
    val gitlabToken = project.findProperty("gitlabToken") as String?
        ?: System.getenv("GITLAB_TOKEN")
    val gitlabUrl = project.findProperty("gitlabUrl") as String?
        ?: "https://gitlab.coinkeep.com/api/v4/projects/$gitlabProjectId/packages/maven"

    if (gitlabToken != null && gitlabProjectId != null) {
        configure<PublishingExtension> {
            repositories {
                maven {
                    name = "GitLab"
                    url = uri(gitlabUrl)
                    credentials(HttpHeaderCredentials::class) {
                        name = "Private-Token"
                        value = gitlabToken
                    }
                    authentication {
                        create("header", HttpHeaderAuthentication::class)
                    }
                }
            }
        }
    } else {
        logger.warn("GitLab publishing skipped: missing gitlabToken or gitlabProjectId")
    }
}
