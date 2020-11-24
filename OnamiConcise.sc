//Run with amm --class-based
import scala.concurrent._, duration.Duration.Inf, java.util.concurrent.Executors 
import $ivy.`com.lihaoyi::requests:0.6.5`
import $ivy.`org.jsoup:jsoup:1.13.1`, org.jsoup._
import $ivy.`org.asynchttpclient:async-http-client:2.5.2`
implicit val ec = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(100))
implicit val asyncHttpClient = org.asynchttpclient.Dsl.asyncHttpClient()

val baseUrl = "http://www.dreams.cash"
val pageUrls = collection.immutable.Set("http://www.dreams.cash/jekyll/update/2020/06/19/welcome-to-jekyll.html", "http://www.dreams.cash/drawings/")

// Or:
Future.traverse(pageUrls) { url => 
  getPage(url)
    .map(page => parseImageSrc(page))
    .map(_.map(imageUrl =>  // I hate this duplicate map here
        downloadFile(fileUrl=imageUrl, outdir=os.pwd / cleanUrl(url))
    ))
}

def getPage(pageUrl: String)(implicit asyncHttpClient: org.asynchttpclient.AsyncHttpClient): Future[org.asynchttpclient.Response] = {
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

def cleanUrl(url: String): String = url.replaceAll("[-+.^:,]","").replaceAll("/", "")

def downloadFile(fileUrl: String, outdir: os.Path) = {
  val UrlPattern = "http(.*)/(.*.[jpeg|jpg|gif|png])".r
  val RelUrlPattern = "/(.*)/(.*.[jpeg|jpg|gif|png])".r
  fileUrl match {
    case UrlPattern(host, file) => os.write(outdir / file, requests.get.stream(f"$host/$file"), createFolders=true)
    case RelUrlPattern(relPath, file) => os.write(outdir / file, requests.get.stream(f"$baseUrl/$relPath/$file"), createFolders=true)
    case _ => println(f"Failed $fileUrl")
  }
}
