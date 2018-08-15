package mesosphere.marathon.example.plugin.auth

import java.util.Base64
import org.mindrot.jbcrypt.BCrypt

import mesosphere.marathon.plugin.auth.{ Authenticator, Identity }
import mesosphere.marathon.plugin.http.{ HttpRequest, HttpResponse }
import mesosphere.marathon.plugin.plugin.PluginConfiguration
import play.api.libs.json.{ JsObject, Json }
import java.io.File

import org.apache.commons.io.FileUtils

import scala.concurrent.Future

class ExampleAuthenticator extends Authenticator with PluginConfiguration {

  import scala.concurrent.ExecutionContext.Implicits.global

  override def handleNotAuthenticated(request: HttpRequest, response: HttpResponse): Unit = {
    response.status(401)
    response.header("WWW-Authenticate", """Basic realm="Marathon Example Authentication"""")
    response.body("application/json", """{"message": "Not Authenticated!"}""".getBytes("UTF-8"))
  }

  override def authenticate(request: HttpRequest): Future[Option[Identity]] = Future {

    def basicAuth(header: String): Option[(String, String)] = {
      val BasicAuthRegex = "Basic (.+)".r
      val UserPassRegex = "([^:]+):(.+)".r
      header match {
        case BasicAuthRegex(encoded) =>
          val decoded = new String(Base64.getDecoder.decode(encoded))
          try {
            val UserPassRegex(username, password) = decoded
            Some(username->password)
          } catch {
            case _: MatchError => None
          }
        case _ => None
      }
    }

    loadIdentities()

    for {
      auth <- request.header("Authorization").headOption
      (username, password) <- basicAuth(auth)
      identity <- identities.get(username) if BCrypt.checkpw(password, identity.password)
    } yield identity

  }

  private var identities = Map.empty[String, ExampleIdentity]
  private var identitiesFileLocation = ""
  private var refreshIdentitiesSeconds = 60
  private var lastUpdateSeconds = 0L

  def loadIdentities() {
    val currentSecond: Long = System.currentTimeMillis / 1000
    if(currentSecond - lastUpdateSeconds > refreshIdentitiesSeconds) {
      val creds = Json.parse(FileUtils.readFileToByteArray(new File(identitiesFileLocation).getCanonicalFile)).as[JsObject]
      identities = (creds \ "users").as[Seq[ExampleIdentity]].map(id => id.username -> id).toMap
      lastUpdateSeconds = currentSecond
    }

  }

  override def initialize(marathonInfo: Map[String, Any], configuration: JsObject): Unit = {
    identitiesFileLocation =  (configuration \ "identitiesFileLocation").as[String]
    refreshIdentitiesSeconds = (configuration \ "refreshIdentitiesSeconds").as[Int]
  }
}
