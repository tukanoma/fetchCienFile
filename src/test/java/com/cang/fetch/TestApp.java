package com.cang.fetch;

import com.cang.fetch.vo.WorksDetailVo;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.ArrayList;

public class TestApp {

    public static void main(String[] args) throws UnsupportedEncodingException {
        ArrayList<WorksDetailVo> page1WorksList = getPage1WorksList(1, "陽向葵ゅか");
        page1WorksList.forEach(detail -> {
            String imageUrl = detail.getImageUrl();
            String imgFileName = imageUrl.substring(imageUrl.lastIndexOf("/"));
            URL url;
            String localUrl = null;
            final String dir = ClassLoader.getSystemClassLoader().getResource("").getPath() + "dlImages";
            try {
                url = new URL(imageUrl);
            } catch (MalformedURLException e) {
                e.printStackTrace();
                throw new RuntimeException("打开链接失败! => " + imageUrl);
            }
            FileOutputStream fileOutputStream = null;
            InputStream inputStream = null;
            try {
                URLConnection urlConnection = url.openConnection();
                inputStream = urlConnection.getInputStream();
                File file = new File(dir);
                if (!file.exists() && !file.isDirectory()) {
                    file.mkdirs();
                }
                localUrl = dir + imgFileName;
                fileOutputStream = new FileOutputStream(localUrl);
                byte[] buf = new byte[1024];
                int readLen = 0;
                while ((readLen = inputStream.read(buf)) != -1) {
                    fileOutputStream.write(buf, 0, readLen);
                }
            } catch (IOException e) {
                e.printStackTrace();
                if (fileOutputStream != null) {
                    try {
                        fileOutputStream.close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
            detail.setImageUrl(localUrl);
        });
        System.out.println("保存到本地完成!");

        page1WorksList.forEach(d -> System.out.println(d));
    }

    public static ArrayList<WorksDetailVo> getPage1WorksList(int pageNo, String authorName) throws UnsupportedEncodingException {
        String dataUrl = "https://www.dlsite.com/maniax/works/type/=/language/jp/sex_category%5B0%" +
                "5D/male/work_category%5B0%5D/doujin/work_type_category%5B0%5D/audio/work_type_category_name%5" +
                "B0%5D/%E3%83%9C%E3%82%A4%E3%82%B9%E3%83%BBASMR/options_and_or/and/options%5B0%5D/JPN/options%5B1%5D/NM/" +
                "per_page/30/show_type/3/lang_options%5B0%5D/%E6%97%A5%E6%9C%AC%E8%AA%9E/lang_options%5B1%5D/%E8%A8%80%" +
                "E8%AA%9E%E4%B8%8D%E8%A6%81/without_order/" + pageNo + "/page/1/order/release_d/";
        if (authorName != null && !authorName.equals("")) {
            dataUrl += "keyword/" + URLDecoder.decode(authorName, "utf-8");
        }
        Connection connect = Jsoup.connect(dataUrl);
        connect.proxy("127.0.0.1", 7890);
        Document document;
        try {
            document = connect.get();
        } catch (IOException e) {
            throw new RuntimeException("连接异常!异常信息=> " + e.getMessage());
        }
        Elements worksList = document.select("#search_result_img_box > li");
        ArrayList<WorksDetailVo> worksDetailVos = new ArrayList<>();
        for (Element work : worksList) {
            WorksDetailVo worksDetailVo = new WorksDetailVo();
            Element makerAndAuthorEle = work.select(".work_img_main > .maker_name").get(0);
            String maker = makerAndAuthorEle.select("a").get(0).text();
            Elements authorTag = makerAndAuthorEle.select(".author");
            String author = null;
            if (authorTag.size() > 0) {
                author = authorTag.get(0).select("a").get(0).text();
            }
            worksDetailVo.setMakerName(maker);
            worksDetailVo.setAuthor(author);
            Element workAndImgUrlEle =
                    work.select(".work_img_main > dt").get(0);
            String workUrl = workAndImgUrlEle.selectFirst("a").attr("href");
            String imageUrl = "https:" + workAndImgUrlEle.selectFirst("a > img").attr("src");
            worksDetailVo.setWorkUrl(workUrl);
            worksDetailVo.setImageUrl(imageUrl);
            worksDetailVos.add(worksDetailVo);
        }
        return worksDetailVos;
    }
}
