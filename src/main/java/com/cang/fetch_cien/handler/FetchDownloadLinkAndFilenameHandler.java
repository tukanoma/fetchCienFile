package com.cang.fetch_cien.handler;

import com.cang.fetch_cien.dto.FileDto;
import com.cang.fetch_cien.exception.FetchException;
import com.cang.fetch_cien.util.ConnectUtil;
import com.cang.fetch_cien.util.ConsTant;
import lombok.Data;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


/**
 * @author -抓取投稿详情页面内的文件名和下载URL
 * create date: 2023/6/22 12:06
 */
@Data
public class FetchDownloadLinkAndFilenameHandler {


    /**
     * 需要处理的投稿详细URL列表,key为页码, value为Url列表
     */
    private Map<Integer, List<String>> allPageArticles;

    /**
     * 储存文件名和文件下载URL,该Map不为空(empty)时,由其他线程开始下载
     */
    private Map<String, FileDto> fileAndFileUrlMap;


    /**
     * 处理失败URL集合,用于重试
     */
    private Set<String> failedUrl;

    /**
     * 处理所有url是否成功
     */
    private boolean processSuccess;

    /**
     * 线程执行器,处理多个投稿列详细中的文件名和文件下载地址,此处主线程会阻塞等待,可以使用最大线程数处理。
     */
    private ThreadPoolExecutor poolExecutor;

    /**
     * 并发量控制
     * */
    private Semaphore semaphore = new Semaphore(2);


    /**
     * 初始化线程池。
     */
    public FetchDownloadLinkAndFilenameHandler(Map<Integer, List<String>> allPageArticles, Map<String, FileDto> fileAndFileUrlMap) {
        if (allPageArticles == null || allPageArticles.isEmpty()) {
            throw new FetchException("作者投稿详细页面不能为空");
        }
        this.allPageArticles = allPageArticles;
        this.fileAndFileUrlMap = fileAndFileUrlMap;

        // ci-en 每页固定15个投稿详细,这里阻塞队列大小15即可不会溢出
        this.poolExecutor =
                new ThreadPoolExecutor(2, 2, 30, TimeUnit.SECONDS,
                        new LinkedBlockingDeque<>(15), new BasicThreadFactory.Builder().build(),
                        new ThreadPoolExecutor.AbortPolicy());
        this.failedUrl = new CopyOnWriteArraySet<>();
        init();
    }


    private void init() {
        allPageArticles.forEach((k, v) -> {
            v.forEach(url -> {
                try {
                    semaphore.acquire();
                    poolExecutor.execute(new FileAndUrlGetter(url));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });

        });


        // 总体重试3次
        int retryCount = 0;
        while (!failedUrl.isEmpty()) {
            failedUrl.forEach(url -> poolExecutor.execute(new FileAndUrlGetter(url)));
            retryCount++;
            if (retryCount >= ConsTant.RETRY_TIMES) {
                return;
            }
        }

        //标记处理成功
        processSuccess = true;
        // 处理完成 关闭线城池
        poolExecutor.shutdown();

    }

    public class FileAndUrlGetter implements Runnable {

        private String url;

        public FileAndUrlGetter(String url) {
            this.url = url;
        }

        @Override
        public void run() {
            Connection connect = Jsoup.connect(this.url);
            ConnectUtil.prepareConnection(connect);

            Document document = null;
            // 失败次数计数
            int failCount = 0;
            // 连接成功标记
            boolean connectionFlag = false;
            while (!connectionFlag) {
                try {
                    document = connect.get();
                    connectionFlag = true;
                } catch (IOException e) {
                    // 连接失败,重试。
                    failCount++;
                    if (failCount >= ConsTant.RETRY_TIMES) {
                        FetchDownloadLinkAndFilenameHandler.this.failedUrl.add(url);
                        return;
                    }
                }
            }
            // 获取投稿发布日期
            String articleReleaseDate = document.select(".e-box.is-paid")
                    .get(0).select(".e-date").get(0).text();
            LocalDate releaseDate = LocalDate.parse(articleReleaseDate, DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"));

            // 获取下载连接和文件名
            Elements elements = document.select(".downloadBlock");
            elements.forEach(download -> {
                String fileName = download.select("p").get(0).text();
                fileName = fileName.substring(0, fileName.lastIndexOf(" ("));
                String url = download.select("a").get(0).attr("href");
                System.out.println("文件名=> " + fileName);
                System.out.println("url=> " + url);
                System.out.println("发布日期=> " + articleReleaseDate);
                System.out.println("localDate=>" + releaseDate);
                FileDto fileDto = new FileDto();
                fileDto.setFileName(fileName);
                fileDto.setFileUrl(url);
                fileDto.setPostedDate(releaseDate);
                FetchDownloadLinkAndFilenameHandler.this.fileAndFileUrlMap.put(fileName, fileDto);
            });

            // semaphore释放
            FetchDownloadLinkAndFilenameHandler.this.semaphore.release();
        }
    }
}
