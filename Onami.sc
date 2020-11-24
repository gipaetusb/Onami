//Run with amm --class-based
import scala.concurrent._, duration.Duration.Inf, java.util.concurrent.Executors 
import $ivy.`com.lihaoyi::requests:0.6.5`
import $ivy.`org.jsoup:jsoup:1.13.1`, org.jsoup._
import $ivy.`org.asynchttpclient:async-http-client:2.5.2`

// Scrape these URLs and download all their images locally
val baseUrl = "http://www.dreams.cash"
val pageUrls = collection.immutable.Set("http://www.dreams.cash/jekyll/update/2020/06/19/welcome-to-jekyll.html", "http://www.dreams.cash/drawings/")

val asyncHttpClient = org.asynchttpclient.Dsl.asyncHttpClient()
implicit val ec = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(100))

val UrlPattern = "http(.*)/(.*.[jpeg|jpg|gif|png])".r
val RelUrlPattern = "/(.*)/(.*.[jpeg|jpg|gif|png])".r

// This still looks acceptable but it does save all the images in the same directory
val imageSrcFutures: Future[Set[String]] = Future.sequence(
  pageUrls.map(url => getPage(url).map(p => parseImageSrc(p)))
).map(_.flatten)
val downloadFutures = imageSrcFutures map {_ map { src =>
  src match {
    case UrlPattern(host, file) => os.write(os.pwd / file, requests.get.stream(f"$host/$file"), createFolders=true)
    case RelUrlPattern(relPath, file) => os.write(os.pwd / file, requests.get.stream(f"$baseUrl/$relPath/$file"), createFolders=true)
    case _ => println(f"Failed $src")
  }
}}

// Download each url to dedicated directory
val imageSrcFutures2: Map[String, Future[Array[String]]] = pageUrls.map(url => (url.replaceAll("[-+.^:,]","").replaceAll("/", ""), getPage(url).map(p => parseImageSrc(p)))).toMap

val downloadFutures2 = for ((page, imageSrcFuture) <- imageSrcFutures2) yield imageSrcFuture map {
  _ map { src =>
    src match {
      case UrlPattern(host, file) => os.write(os.pwd / page / file, requests.get.stream(f"$host/$file"), createFolders=true)
      case RelUrlPattern(relPath, file) => os.write(os.pwd / page / file, requests.get.stream(f"$baseUrl/$relPath/$file"), createFolders=true)
      case _ => println(f"Failed $src")
    }
  }
}

def getPage(pageUrl: String): Future[org.asynchttpclient.Response] = {
  val p = Promise[org.asynchttpclient.Response]
  val listenableFut = asyncHttpClient.prepareGet(pageUrl).execute()
  listenableFut.addListener(() => p.success(listenableFut.get()), null)
  p.future
}

def parseImageSrc(page: org.asynchttpclient.Response): Array[String] = { 
  Jsoup.parse(page.toString)
    .select("img")
    .eachAttr("src")
    .toArray
    .map(_.asInstanceOf[String])
}

// Not used
def parseImageSrcFuture(futPage: Future[org.asynchttpclient.Response]): Future[Array[String]] = {
  futPage map { page =>
      Jsoup.parse(page.toString)
        .select("img")
        .eachAttr("src")
        .toArray
        .map(_.asInstanceOf[String])
  }
}
