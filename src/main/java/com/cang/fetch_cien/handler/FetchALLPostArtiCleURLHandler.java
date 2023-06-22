package com.cang.fetch_cien.handler;

import com.cang.fetch_cien.exception.FetchException;
import com.cang.fetch_cien.util.ConnectUtil;
import com.cang.fetch_cien.util.ConsTant;
import com.cang.fetch_cien.util.PropertyScope;
import lombok.Data;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author 抓取配置文件中creatorArticlesPage指定的作者的所有投稿的URL
 * create date: 2023/6/22 14:48
 */
@Data
public class FetchALLPostArtiCleURLHandler {

    /**
     * 当前页面能看到的最大页码数
     */
    private int maxPage;


    /**
     * 所有投稿的列表,key为页码,value为每页的投稿详细Url列表
     */
    private Map<Integer, List<String>> allPageArticles;

    /**
     * 获取投稿列表失败的页码。
     */
    private Set<Integer> failedPage;


    /**
     * 投稿者的投稿列表页面,默认第一页开始
     */
    private String creatorArticlesPage = PropertyScope.getProperty("creatorArticlesPage");

    /**
     * 线程执行器,处理多个投稿列表页面中需要收集的投稿详细URL,此处主线程会阻塞等待,可以使用最大线程数处理。
     */
    private ThreadPoolExecutor poolExecutor;

    /**
     * 处理所有投稿是否成功
     */
    private boolean processSuccess;

    /**
     * 初始化最大页码,线程池
     * */
    public FetchALLPostArtiCleURLHandler(Map<Integer, List<String>> allPageArticles) {
        // 跳转到999页,理论上应该不存在发布了这么多投稿的投稿者,目的是获取最大页码
        String url = creatorArticlesPage + 999;

        Connection connect = Jsoup.connect(url);
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
                    throw new FetchException("连接投稿详细页面" + this.creatorArticlesPage + "失败..");
                }
            }
        }
        Elements pagerItems = document.select(".pagerItem");
        this.maxPage = Integer.parseInt(pagerItems.get(pagerItems.size() - 2).text());
        this.failedPage = new CopyOnWriteArraySet<>();
        this.poolExecutor =
                new ThreadPoolExecutor(3, 3, 30, TimeUnit.SECONDS,
                        new LinkedBlockingDeque<>(maxPage), new BasicThreadFactory.Builder().build(),
                        new ThreadPoolExecutor.AbortPolicy());
        this.allPageArticles = allPageArticles;
        init();
    }


    public void init() {
        for (int i = 1; i <= maxPage; i++) {
            poolExecutor.execute(new ArticleListGetter(i));
        }

        // 未处理完成,等待
        while (poolExecutor.getActiveCount() != 0) {
            try {
                TimeUnit.SECONDS.sleep(3);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("处理失败页码=>" + failedPage);

        // 总体重试3次
        int retryCount = 0;
        while (!failedPage.isEmpty()) {
            failedPage.forEach(i -> poolExecutor.execute(new ArticleListGetter(i)));
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


    public class ArticleListGetter implements Runnable {

        private int currentPage;

        public ArticleListGetter(int currentPage) {
            this.currentPage = currentPage;
        }

        @Override
        public void run() {
            String url = FetchALLPostArtiCleURLHandler.this.creatorArticlesPage + currentPage;
            Connection connect = Jsoup.connect(url);
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
                        System.out.printf("处理第: %d 页失败\n", currentPage);
                        FetchALLPostArtiCleURLHandler.this.failedPage.add(currentPage);
                        return;
                    }
                }
            }

            // 每页15个
            Elements articleElements = document.select(".c-cardCase.is-multiCell").select(".c-postedArticle");
            List<String> articleList = articleElements.stream()
                    .map(articleElement -> articleElement.select("a").get(0).attr("href"))
                    .collect(Collectors.toList());
            FetchALLPostArtiCleURLHandler.this.allPageArticles.put(currentPage, articleList);


            //重试成功 移除处理失败页码
            if (FetchALLPostArtiCleURLHandler.this.failedPage.contains(currentPage)) {
                FetchALLPostArtiCleURLHandler.this.failedPage.remove(currentPage);
            }
            System.out.printf("处理第: %d 页完成\n", currentPage);
        }
    }

}
