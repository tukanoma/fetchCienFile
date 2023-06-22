package com.cang.fetch_cien;


import com.cang.fetch_cien.dto.FileDto;
import com.cang.fetch_cien.handler.FetchDownloadLinkAndFilenameHandler;
import com.cang.fetch_cien.handler.FetchALLPostArtiCleURLHandler;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author yue
 * create date: 2023/6/22 11:32
 */
public class FetchApplication {
    public static void main(String[] args) throws InterruptedException {
        FetchApplication fetchApplication = new FetchApplication();
    }


    /**
     * 线程执行器,当获取所有作者投稿详细的URL之后,调用该执行器执行 FetchThreadHandler.run()
     * 处理投稿详细的URL中包含的文件名和文件下载URL
     *
     * @see FetchDownloadLinkAndFilenameHandler
     */
    private ThreadPoolExecutor poolExecutor =
            new ThreadPoolExecutor(3, 5, 30, TimeUnit.SECONDS,
                    new LinkedBlockingDeque<>(3), new BasicThreadFactory.Builder().build(),
                    new ThreadPoolExecutor.AbortPolicy());


    /**
     * 所有投稿详细URL列表，Key为页码,value为URL的列表(未处理之前为空)
     */
    private final Map<Integer, List<String>> allPageArticles = new ConcurrentHashMap<>();

    /**
     * 储存文件名和文件下载URL,该Map不为空(empty)时,由其他线程开始下载(未处理之前为空)
     */
    private final Map<String, FileDto> fileAndFileUrlMap = new ConcurrentHashMap<>();

    /**
     * 获取所有投稿详细URL列表的处理器
     */
    private FetchALLPostArtiCleURLHandler postArtiCleHandler;


    /**
     * 获取所有投稿详细中的文件名和下载地址
     */
    private FetchDownloadLinkAndFilenameHandler fetchThreadHandler;


    /**
     * 初始化工作在无参构造器完成
     */
    public FetchApplication() throws InterruptedException {
        postArtiCleHandler = new FetchALLPostArtiCleURLHandler(allPageArticles);

        // 列表未处理完成前,等待。
        while (!postArtiCleHandler.getPoolExecutor().isShutdown()) {
            TimeUnit.SECONDS.sleep(3);
        }

        // 列表处理失败,退出
        if (!postArtiCleHandler.isProcessSuccess()) {
            return;
        }

        fetchThreadHandler = new FetchDownloadLinkAndFilenameHandler(allPageArticles, fileAndFileUrlMap);



        //TODO 此处可以启动DownloadMapListener监听器,直接开始下载fileAndFileUrlMap集合里的文件,注意每次取出一个K - V值,
        // 如果下载成功从集合中移除该K-V 下载失败可以重试下载,重试次数使用 ConnectUtil.RETRY_TIMES

        // 文件获取处理未完成前,等待。
        while (!fetchThreadHandler.getPoolExecutor().isShutdown()) {
            TimeUnit.SECONDS.sleep(3);
        }

        // 文件获取处理失败,退出
        if (!fetchThreadHandler.isProcessSuccess()) {
            return;
        }

        System.out.println("处理OK");
    }
}
