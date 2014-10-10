import spray.json._
import spray.httpx._
import spray.routing._
import spray.testkit._
import org.scalatest._

trait MultiParamsSupport {
  import spray.httpx.unmarshalling.{ FromStringDeserializer ⇒ FSD, _ }
  import spray.routing.directives._
  import BasicDirectives._
  import RouteDirectives._

  implicit def forNameReceptacleAsList[T](implicit fsd: FSD[T]) = new ParamDefMagnet2[NameReceptacle[List[T]]] {
    type Out = Directive1[List[T]]
    def apply(nr: NameReceptacle[List[T]]): Directive1[List[T]] =
      extract(_.request.uri.query.getAll(nr.name) map fsd.apply) flatMap { xs ⇒
        xs.foldLeft[Either[Rejection, List[T]]](Right(List.empty)) { (acc, nxt) ⇒
          nxt match {
            case Right(x)                             ⇒ acc.right map (x :: _)
            case Left(ContentExpected)                ⇒ acc.right flatMap (_ ⇒ Left(MissingQueryParamRejection(nr.name)))
            case Left(MalformedContent(error, cause)) ⇒ acc.right flatMap (_ ⇒ Left(MalformedQueryParamRejection(nr.name, error, cause)))
            case Left(x: UnsupportedContentType)      ⇒ throw new IllegalStateException(x.toString)
          }
        } match {
          case Right(xs)       ⇒ provide(xs)
          case Left(rejection) ⇒ reject(rejection)
        }
      }
  }
}

class Main extends FunSpec
    with ShouldMatchers
    with ScalatestRouteTest
    with HttpService
    with SprayJsonSupport
    with DefaultJsonProtocol
    with MultiParamsSupport {

  def actorRefFactory = system

  val route =
    parameter('id.as[List[Int]]) { (id) ⇒
      complete {
        id
      }
    }

  it("single param") {
    Get("?id=1&color=red") ~> route ~> check {
      responseAs[List[Int]] shouldBe List(1)
    }
  }
  it("multi param") {
    Get("?id=2&id=3") ~> route ~> check {
      responseAs[List[Int]] shouldBe List(2, 3)
    }
  }
  it("no param") {
    Get("?") ~> route ~> check {
      responseAs[List[Int]] shouldBe Nil
    }
  }
  it("deserialise error") {
    Get("?id=1&id=a") ~> route ~> check {
      rejection.asInstanceOf[MalformedQueryParamRejection].parameterName shouldBe "id"
    }
  }
}
