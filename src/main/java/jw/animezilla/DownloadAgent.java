package jw.animezilla;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Created by tech0251 on 2018/5/2.
 */
public class DownloadAgent {

    public static void main(String[] args) throws Exception {
        File file = new File("/comic");

        if (!file.exists()) {
            file.mkdirs();
        }

        DownloadAgent downloadAgent = new DownloadAgent();

        downloadAgent.run(file.getAbsolutePath(), "http://18h.animezilla.com/manga/3218");
    }

    public void run(String rootPath, String url) throws Exception {
        boolean hasNext = false;

        int idx = 0;

        String title = null;

        do {
            HttpRequester httpRequester = new HttpRequester(url);
            httpRequester.requestHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/66.0.3359.139 Safari/537.36");
            httpRequester.requestHeader("Referer", url);

            Document doc = Jsoup.parse(httpRequester.get().html());


            //create folder by title
            if (idx == 0) {
                title = doc.body().select("h1.entry-title").text();
                title = title.replace("/", " ");
                title = title.substring(0, title.lastIndexOf("-"));

                rootPath = rootPath + "/" + title.trim();

                new File(rootPath).mkdir();
            }


            //extract image url
            String imageUrl = doc.body().select("#comic").attr("src");

            save(url, imageUrl, new File(rootPath + "/" + title + "-" + idx + ".jpg"));


            //extract next page
            String nextUrl = doc.body().select("a.nextpostslink").attr("href");
            url = nextUrl;


            //check has next page
            if (nextUrl != null && !nextUrl.isEmpty()) {
                hasNext = true;
                idx++;
            } else {
                hasNext = false;
            }
        } while (hasNext);
    }

    public void save(String url, String imageUrl, File file) throws IOException {
        HttpRequester httpRequester = new HttpRequester(imageUrl);
        httpRequester.requestHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/66.0.3359.139 Safari/537.36");
        httpRequester.requestHeader("Referer", url);

        BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(httpRequester.get().raw()));

        ImageIO.write(bufferedImage, "jpg", file);
    }
}

class HttpRequester {

    private URL urlObj = null;

    private URLConnection urlConnection = null;

    private byte[] raw = new byte[8 * 1024];

    public HttpRequester(String url) throws IOException {
        this(new URL(url));
    }

    public HttpRequester(URL url) throws IOException {
        urlObj = url;

        urlConnection = urlObj.openConnection();
        urlConnection.setDefaultUseCaches(false);
        urlConnection.setUseCaches(false);
    }

    public HttpRequester requestHeader(String key, String val) {
        urlConnection.setRequestProperty(key, val);

        return this;
    }

    public HttpRequester get() {
        BufferedInputStream stream = null;

        //retry 3 times
        for (int i = 0; i < 3; i++) {
            try {

                //
                if (((HttpURLConnection) urlConnection).getResponseCode() != 200) {
                    System.out.println("'" + urlObj.toString() + "' response not 200, retry..." + (i + 1));
                    Thread.sleep(1000 * (i + 1));
                    continue;
                }
//Jsoup.connect("").execute();
                stream = new BufferedInputStream(urlConnection.getInputStream());

                byte[] buff = new byte[8 * 1024];

                int total = 0;

                int offset = 0;

                while ((offset = stream.read(buff)) > -1) {
                    if (offset + total > raw.length) {
                        raw = Arrays.copyOf(raw, raw.length * 2);
                    }

                    System.arraycopy(buff, 0, raw, total, offset);

                    total = total + offset;
                }

                break;
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        return this;
    }

    public Map<String, List<String>> responseHeaders() {
        return urlConnection.getHeaderFields();
    }

    public void responseHeader() {

    }

    public String html() {
        return new String(raw);
    }

    public byte[] raw() {
        return raw;
    }
}