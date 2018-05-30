package crawler_final;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.lucene.search.spell.JaroWinklerDistance;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Crawler_final {

    public static void main(String[] args) {
        try {

            Q1("Minecraft: PlayStation4 Edition");

        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void Q1(String keyword)throws IOException {

         Map<String, Double> confidenceMap = new HashMap<>();
         Map<String, String> urlMap = new HashMap<>();
         Map<String, String> imgMap = new HashMap<>();




//        List<String> titleList = new ArrayList<>();
//        List<Double> confidenceList = new ArrayList<>();
//        List<>
//

        String rootUrl = "https://search.rakuten.co.jp/search/mall/" + keyword + "/";
        Document doc = Jsoup.connect(rootUrl).get();

        String[] last = doc.select("span.count._medium").text().split("件");
        int lastItemPage = Integer.parseInt(last[1].replace(",", "").replace("（", "").replace(" ", "")) /45 + 1;
        System.out.println("last=" + lastItemPage); //最後のページを求める

        if (lastItemPage > 150) {
            lastItemPage = 150;
        }

        String title = null;
        String itemUrl = null;
        String imgUrl = null;
        
        for (int i = 1; i <= lastItemPage; i++) {

            try {
                Thread.sleep(1000);
            }catch(InterruptedException e) {
                System.out.println(e);
            }

            String itemPageUrl = "https://search.rakuten.co.jp/search/mall/" + keyword + "/?=" + i;
            Document doc2 = Jsoup.connect(itemPageUrl).get();
            Elements items = doc2.select("div.dui-card.searchresultitem");


            if (items.isEmpty()) {
                break;
            }

            double confidenceA = 0.0;
            double confidenceB = 0.0;

            for (int z = 0; z < items.size(); z++) {


                title = items.get(z).select("div.content.title > h2 > a").attr("title");
                itemUrl = items.get(z).select("div.content.title > h2 > a").attr("href");
                imgUrl = items.get(z).select("img._verticallyaligned").attr("src");
                
                String reviewPage = items.get(z).select("a.dui-rating-filter._link").attr("href");

                if(reviewPage.isEmpty()) {
                    continue;
                }else {
                    System.out.println(i + " : " +title);
                }

                String url = reviewPage.substring(0,reviewPage.indexOf("/", 40));
                System.out.println(url);
                List<Integer> ratingList = new ArrayList<>();
                Map<Integer, Integer> numberMap = new HashMap<>();

                try {
                    Thread.sleep(1000);
                }catch(InterruptedException e) {
                    System.out.println(e);
                }

                int lastPage = 0;
                double sum = 0;

                Document first = Jsoup.connect(url + "/1.1/").get();




                Elements numbers = first.select("li.revChartDevList");

                String[] fiveNum = numbers.get(0).select("span.revChartDevNum").text().split("件");
                String[] fourNum = numbers.get(1).select("span.revChartDevNum").text().split("件");
                String[] threeNum = numbers.get(2).select("span.revChartDevNum").text().split("件");
                String[] twoNum = numbers.get(3).select("span.revChartDevNum").text().split("件");
                String[] oneNum = numbers.get(4).select("span.revChartDevNum").text().split("件");

                int five = Integer.parseInt(fiveNum[0].replace(",", ""));
                int four = Integer.parseInt(fourNum[0].replace(",", ""));
                int three = Integer.parseInt(threeNum[0].replace(",", ""));
                int two = Integer.parseInt(twoNum[0].replace(",", ""));
                int one = Integer.parseInt(oneNum[0].replace(",", ""));

                numberMap.put(5, five);
                numberMap.put(4, four);
                numberMap.put(3, three);
                numberMap.put(2, two);
                numberMap.put(1, one);




                List<String> intervalList = new ArrayList<>();


                for(int y = 1; y >= 1; y++) {

                    try {
                        String pageUrl = url + "/" + y + ".1/"; //1→y
                        Document pageDoc = Jsoup.connect(pageUrl).get();
                        Elements reviews = pageDoc.select("div.revRvwUserSec.hreview");



                        if(reviews.isEmpty()) {

                            lastPage = y-1;
                            break;

                        }else if(y == 101){
                            break;
                        }else {

                            for(Element review : reviews) {
                                int rating = Integer.parseInt(review.select("span.revUserRvwerNum.value").text());

    //                            System.out.println(y +" : "+ rating);
                                sum += rating;
                                ratingList.add(rating);

                                String comment = review.select("dd.revRvwUserEntryCmt.description").get(0).text();

                                if (review.select("dt.revUserFaceName").get(0).child(0).text().contains("購入者")) {
                                    continue;
                                }else {

                                    String reviewer = review.select("dt.revUserFaceName.reviewer > a").get(0).attr("href");
                                    Document reviewerUrl = Jsoup.connect(reviewer).get();
                                    Elements individReviews = reviewerUrl.select("div.goodsReview");

                                    for(Element individReview : individReviews) { //他のレビューとの文字列の類似度を測定

                                        int count = 2;
                                        JaroWinklerDistance jwd = new JaroWinklerDistance();
                                        String text = individReview.select("div.reviewText > p.text").text();

//                                        System.out.println(jwd.getDistance(text, comment));
                                        if(jwd.getDistance(text, comment) >  0.7) {
                                            count--;
                                        }

                                        if(count == 0) {

                                            numberMap.put(rating, numberMap.get(rating) - 1);

                                        }

                                    }



                                }
                            }


                            try {
                                Thread.sleep(1000);
                            }catch(InterruptedException e) {
                                System.out.println(e);
                            }



                        }

                    }catch(IOException e) {

                        e.printStackTrace();

                    }

                }

                List <Double> deviationList = new ArrayList<>();
                double allReview = numberMap.get(5) + numberMap.get(4) + numberMap.get(3) + numberMap.get(2) + numberMap.get(1); //スパム（と思われるもの）を除いた総レビュー数
                double average = (5 * numberMap.get(5) + 4 * numberMap.get(4) + 3 * numberMap.get(3) + 2 * numberMap.get(2) + 1 * numberMap.get(1)) /allReview; //標本平均

//                System.out.println(allReview);
//
//                System.out.println(average);
                Double devSum = 0.0;

                //不偏分散算出のため、偏差の2乗の合計を求める
                for(int x = 0; x < numberMap.get(5); x++) {
                    double deviation = Math.pow(Math.abs(5 - average), 2);
                    deviationList.add(deviation);
                }

                for(int x = 0; x < numberMap.get(4); x++) {
                    double deviation = Math.pow(Math.abs(4 - average), 2);
                    deviationList.add(deviation);
                }

                for(int x = 0; x < numberMap.get(3); x++) {
                    double deviation = Math.pow(Math.abs(3 - average), 2);
                    deviationList.add(deviation);
                }

                for(int x = 0; x < numberMap.get(2); x++) {
                    double deviation = Math.pow(Math.abs(2 - average), 2);
                    deviationList.add(deviation);
                }

                for(int x = 0; x < numberMap.get(1); x++) {
                    double deviation = Math.pow(Math.abs(1 - average), 2);
                    deviationList.add(deviation);
                }

                for(double div :deviationList) {
                    devSum += div;


                }

                int freedom = (int)allReview - 1;

                if(freedom == 0) {
                    continue;
                }

                double standardDeviation = (double)devSum / (freedom); //不偏分散
                double t = 0.0;



                //t分布表からtを求める
                if(freedom == 1) {
                     t = 12.706;

                }else if(freedom == 2) {
                     t = 4.303;

                }else if (freedom == 3) {
                     t = 3.182;

                }else if (freedom == 4) {
                     t = 2.776;

                }else if (freedom == 5) {
                     t = 2.571;

                }else if (freedom == 6) {
                     t = 2.447;

                }else if (freedom == 7) {
                     t = 2.365;

                }else if (freedom == 8) {
                     t = 2.306;

                }else if (freedom == 9) {
                     t = 2.262;

                }else if (freedom == 10) {
                     t = 2.228;

                }else if (freedom == 11) {
                     t = 2.201;

                }else if (freedom == 12) {
                     t = 2.179;

                }else if (freedom == 13) {
                     t = 2.160;

                }else if (freedom == 14) {
                     t = 2.145;

                }else if (freedom == 15) {
                     t = 2.131;

                }else if (freedom == 16) {
                     t = 2.120;

                }else if (freedom == 17) {
                     t = 2.110;

                }else if (freedom == 18) {
                     t = 2.101;

                }else if (freedom == 19) {
                     t = 2.093;

                }else if (freedom == 20) {
                     t = 2.086;

                }else if (freedom == 21) {
                     t = 2.080;

                }else if (freedom == 22) {
                     t = 2.074;

                }else if (freedom == 23) {
                     t = 2.069;

                }else if (freedom == 24) {
                     t = 2.064;

                }else if (freedom == 25) {
                     t = 2.060;

                }else if (freedom == 26) {
                     t = 2.056;

                }else if (freedom == 27) {
                     t = 2.052;

                }else if (freedom == 28) {
                     t = 2.048;

                }else if (freedom == 29) {
                     t = 2.045;

                }else if (freedom == 30) {
                     t = 2.042;

                }else if ((freedom >30) && (freedom <= 40)) {
                     t = 2.021;

                }else if ((freedom > 40) && (freedom <=60)){
                     t = 2.000;

                }else if ((freedom > 60) && (freedom <= 120)) {
                     t = 1.980;

                }else if ((freedom > 120) && (freedom <= 240)) {
                     t = 1.970;

                }else {
                     t = 1.960;

                }




                confidenceA = average + (t * standardDeviation / Math.sqrt(allReview));
                confidenceB = average - (t * standardDeviation / Math.sqrt(allReview));

                System.out.println(confidenceA + "～" + confidenceB);
                intervalList.add((confidenceA + "～" + confidenceB).toString());
                System.out.println(intervalList + "\n");



                if(confidenceB == 5.0) {

                    continue;

                }else {

                    confidenceMap.put(title, confidenceB);
                    urlMap.put(title, itemUrl);
                    imgMap.put(title, imgUrl);


                }

            }





        }


        //Valueでソート
        List<Entry<String,Double>> list = new ArrayList<Entry<String,Double>>(confidenceMap.entrySet());

        Collections.sort(list, new Comparator<Entry<String, Double>>() {

            public int compare(Entry<String, Double> obj1, Entry<String, Double> obj2)
            {

                return obj2.getValue().compareTo(obj1.getValue());
            }
        });

        
        Path answerPath = Paths.get("c:/TechTraining/resources/crawler_final.html"); // 書き込み対象ファイルの場所を指定
        
        try {

            Files.deleteIfExists(answerPath); // 既に存在してたら削除
            Files.createFile(answerPath); // ファイル作成


            //結果の書き込み
            try(BufferedWriter bw = Files.newBufferedWriter(answerPath)) {

                //1行ごとに結果を書き込む
                



                    bw.write("<!DOCTYPE html>" + "\n" + "<html lang=\"ja\">");
                    bw.write("<head>" + "\n"  + "<title>" + keyword + "ランキング" + "</title>" + "\n" +  "<meta charset=\"UTF-8\">" + "\n" +  "</head>" + "\n");

                    bw.write("<body>" + "\n" + "<h1>" + keyword + "ランキング" + "</h1>");
                    bw.newLine();;
                    
                    bw.write("<ul>" + "\n");


                    for(int y = 0; y < list.size(); y++) {
                        
                  
                        bw.write("<div style=\"padding: 10px; margin-bottom: 10px; border: 5px double #333333;\">" + "<a href=\"" + urlMap.get(list.get(y).getKey()) + "\">" + (y + 1) + "位" + " : " + list.get(y).getKey() + "</a>");
                        bw.newLine();
                        bw.write("<div align=\"left\"" + "\n" + "<img src=" + imgMap.get(list.get(y).getKey()) + ">" + "\n" + "</div>");
                        bw.write("\n" + "</div>");

                        if(y == 50) {
                            break;
                        }
                        
                    }
                    
                    bw.write("</ul>");


                    bw.write("</body>");



                

            }

        } catch (IOException e) {
            System.out.println(e);
        }

        
        



    }
}
