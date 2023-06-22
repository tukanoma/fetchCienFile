package com.cang.fetch_cien.handler;

import com.cang.fetch_cien.dto.FileDto;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author 监听文件下载列表中是否有需要下载的文件
 * create date: 2023/6/22 14:39
 */
public class DownloadMapListener implements Runnable {


    /**
     * 用于获取抓取URL内的下载地址和文件名是否完毕。
     */
    private FetchDownloadLinkAndFilenameHandler fetchDownloadLinkAndFilenameHandler;

    /**
     * 等待下载的文件列表 需要和FetchThreadHandler类中的fileAndFileUrlMap字段为同一个对象
     *
     * @see FetchDownloadLinkAndFilenameHandler
     */
    private ConcurrentHashMap<String, FileDto> fileAndFileUrlMap;

    /**
     * 线程执行器,处理fileAndFileUrlMap中需要下载的文件,注意线程数和获取投稿详细的线程数不能超出系统处理上限。
     *
     * @see FetchDownloadLinkAndFilenameHandler
     */
    private ThreadPoolExecutor poolExecutor =
            new ThreadPoolExecutor(5, 7, 30, TimeUnit.SECONDS,
                    new LinkedBlockingDeque<>(3), new BasicThreadFactory.Builder().build(),
                    new ThreadPoolExecutor.AbortPolicy());

    /**
     * 下载失败文件集合
     */
    private Set<FileDto> failedFileSet;

    public DownloadMapListener(ConcurrentHashMap<String, FileDto> fileAndFileUrlMap
            , FetchDownloadLinkAndFilenameHandler fetchDownloadLinkAndFilenameHandler) {
        this.fileAndFileUrlMap = fileAndFileUrlMap;
        this.fetchDownloadLinkAndFilenameHandler = fetchDownloadLinkAndFilenameHandler;
        this.failedFileSet = new CopyOnWriteArraySet<>();
    }


    @Override
    public void run() {
        // 每隔3秒钟查看一次是否有下载任务
        while (!fetchDownloadLinkAndFilenameHandler.isProcessSuccess()) {
            if (!fileAndFileUrlMap.isEmpty()) {
                // TODO 下载文件 成功后移除K-V 失败后放入failedFileSet
                // TODO 下载文件的线程类还没写,可以和另外两个handler类一样,写个内部类操作
            }

            try {
                TimeUnit.SECONDS.sleep(3);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // 获取抓取URL内的下载地址和文件名完毕,处理剩余所有未下载文件
        while (!fileAndFileUrlMap.isEmpty()) {
            // TODO 下载文件 成功后移除K-V 失败后放入failedFileSet
        }


        // TODO 如果需要重试下载失败的文件,在这里加逻辑

    }
}
