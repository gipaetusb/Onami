//Run with amm --class-based
import scala.concurrent._, duration.Duration.Inf, java.util.concurrent.Executors 
import $ivy.`com.lihaoyi::requests:0.6.5`
import $ivy.`org.jsoup:jsoup:1.13.1`, org.jsoup._
import $ivy.`org.asynchttpclient:async-http-client:2.5.2`

val baseUrl = "http://www.dreams.cash"
val pageUrls = collection.immutable.Set("http://www.dreams.cash/jekyll/update/2020/06/19/welcome-to-jekyll.html", "http://www.dreams.cash/drawings/")

val asyncHttpClient = org.asynchttpclient.Dsl.asyncHttpClient()
implicit val ec = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(100))

val UrlPattern = "http(.*)/(.*.[jpeg|jpg|gif|png])".r
val RelUrlPattern = "/(.*)/(.*.[jpeg|jpg|gif|png])".r
val imageSrcFutures: Future[Set[String]] = Future.sequence(
  pageUrls.map(url => getPage(url).map(p => parseImageSrc(p)))
).map(_.flatten)
val downloadfs = imageSrcFutures map {_ map { src =>
  src match {
    case UrlPattern(host, file) => os.write(os.pwd / file, requests.get.stream(f"$host/$file"), createFolders=true)
    case RelUrlPattern(relPath, file) => os.write(os.pwd / file, requests.get.stream(f"$baseUrl/$relPath/$file"), createFolders=true)
    case _ => println(f"Failed $src")
  }
}}

//pageFutures.flatMap(page => parseImages(page).flatMap(img => downloadImage(img)))

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
    // .filter(imgUrl => imgUrl.endsWith(someExt))
}

def parseImageSrc2(page: org.asynchttpclient.Response): Future[Array[String]] = Future { 
  Jsoup.parse(page.toString)
    .select("img")
    .eachAttr("src")
    .toArray
    .map(_.asInstanceOf[String])
    .filter(imgUrl => imgUrl.endsWith("jpg"))
}

def parseImageSrc3(futPage: Future[org.asynchttpclient.Response]): Future[Array[String]] = {
  futPage map { page =>
      Jsoup.parse(page.toString)
        .select("img")
        .eachAttr("src")
        .toArray
        .map(_.asInstanceOf[String])
        .filter(imgUrl => imgUrl.endsWith("jpg"))
  }
}

//def downloadImage(imgUrl: Future[String]) = {
//  imgUrl map {
//    
//  } 
//}
