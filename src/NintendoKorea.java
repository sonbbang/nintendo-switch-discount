import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import model.GameInfo;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class NintendoKorea {
    private static final Logger log = LoggerFactory.getLogger(NintendoKorea.class);
    
    private static final String BASE_URL = "https://store.nintendo.co.kr/all-product";
    private static final String PRODUCT_SELECTOR = ".product-item";
    private static final String LINK_SELECTOR = ".product-item-link";
    private static final String PHOTO_SELECTOR = ".product-image-photo";
    private static final String NEXT_PAGE_SELECTOR = ".next";
    private static final int MIN_DISCOUNT_PERCENT = 70;
    private static final int PRODUCTS_PER_PAGE = 12;
    
    public static void main(String[] args) {
        try {
            List<GameInfo> gameInfos = crawlGameInfos();
            generateHtmlReport(gameInfos);
        } catch (Exception e) {
            log.error("프로그램 실행 중 오류 발생", e);
        }
    }

    private static List<GameInfo> crawlGameInfos() throws IOException {
        List<GameInfo> gameInfos = new ArrayList<>();
        List<String> titles = new ArrayList<>();
        int pageNumber = 1;
        int totalCount = 0;

        while (true) {
            String url = buildPageUrl(pageNumber);
            log.info("크롤링 URL: [{}]", url);

            Document doc = Jsoup.connect(url).get();
            List<GameInfo> pageGameInfos = parseProducts(doc, totalCount, titles);
            
            if (pageGameInfos.isEmpty()) {
                break;
            }

            gameInfos.addAll(pageGameInfos);
            totalCount += pageGameInfos.size();

            if (!checkNextPage(doc)) {
                break;
            }
            pageNumber++;

        }

        return gameInfos;
    }

    private static String buildPageUrl(int pageNumber) {
        return String.format("%s?am_on_sale=1&p=%d&product_list_dir=asc&product_list_limit=%d&product_list_order=price",
                BASE_URL, pageNumber, PRODUCTS_PER_PAGE);
    }

    private static List<GameInfo> parseProducts(Document doc, int startIndex, List<String> titles) throws IOException {
        List<GameInfo> gameInfos = new ArrayList<>();
        Elements products = doc.select(PRODUCT_SELECTOR);
        Elements links = doc.select(LINK_SELECTOR);
        Elements photos = doc.select(PHOTO_SELECTOR);

        for (int i = 0; i < products.size(); i++) {
            Element product = products.get(i);
            GameInfo gameInfo = parseProduct(product, links.get(i), photos.get(i), startIndex + i);
            
            if (!titles.contains(gameInfo.getTitle())) {
                getDetailPage(gameInfo);
                gameInfos.add(gameInfo);
                titles.add(gameInfo.getTitle());
                log.info("게임 정보 파싱 완료: [{}]", gameInfo);
            }
        }

        return gameInfos;
    }

    private static GameInfo parseProduct(Element product, Element link, Element photo, int index) {
        String[] strings = product.text().split(" ");
        int length = strings.length;

        String released = strings[0].replace("발매", "");
        String title = String.join(" ", Arrays.copyOfRange(strings, 2, length - 4));
        String sale = strings[length - 3];
        String price = strings[length - 1];

        GameInfo gameInfo = new GameInfo();
        gameInfo.setIndex(index);
        gameInfo.setDiscountPrice(parseCurrency(sale));
        gameInfo.setPrice(parseCurrency(price));
        gameInfo.setDiscountPercent(calculateDiscountPercent(gameInfo.getPrice(), gameInfo.getDiscountPrice()));
        gameInfo.setResultPrice(gameInfo.getDiscountPrice());
        gameInfo.setNintendoDetailUrl(link.attr("href"));
        gameInfo.setImage(photo.attr("src"));
        gameInfo.setTitle(title.trim());
        gameInfo.setReleased(released);

        return gameInfo;
    }

    private static int calculateDiscountPercent(int originalPrice, int discountPrice) {
        return (int) (((double) originalPrice - (double) discountPrice) / (double) originalPrice * 100.0);
    }
    private static void getDetailPage(GameInfo gameInfo) {
        try {
            // 랜덤 딜레이 (1~10초)
            TimeUnit.SECONDS.sleep(new Random().nextInt(10) + 1);

            Document doc = Jsoup.connect(gameInfo.getNintendoDetailUrl()).get();
            Elements vals = doc.select(".product-attribute-val");

            gameInfo.setDesc(vals.get(0).text());
            gameInfo.setGenre(vals.get(3).text());
            gameInfo.setReleased(vals.get(4).text());
            gameInfo.setPlayers(vals.get(6).text().replace("✕ ", ""));

            if (vals.size() >= 8) {
                gameInfo.setSupportKorean(vals.get(7).text().contains("한국어"));
            }

            gameInfo.setSalePeriod(extractSalePeriod(doc));
        } catch (Exception e) {
            log.error("상세 페이지 파싱 중 오류 발생: {}", gameInfo.getTitle(), e);
        }
    }

    private static String extractSalePeriod(Document doc) {
        return doc.select(".eshop-price-box")
                .text()
                .replace("(세일 기간: ", "")
                .replace(")", "");
    }

    private static boolean checkNextPage(Document doc) {
        return !doc.select(NEXT_PAGE_SELECTOR).isEmpty();
    }

    private static int parseCurrency(String currencyText) {
        String cleanText = currencyText.replaceAll("[^\\d]", "");
        return cleanText.isEmpty() ? 0 : Integer.parseInt(cleanText);
    }

    private static void generateHtmlReport(List<GameInfo> gameInfos) throws IOException {
        String filePath = generateFilePath();
        
        // HTML 파일 생성
        saveHtml(filePath, assembleGuideHtml(), false);
        saveHtml(filePath, assembleStartHtml(), true);

        // 가격순 정렬 후 할인율 높은 순으로 2차 정렬
        gameInfos.sort(Comparator
                .comparing(GameInfo::getResultPrice)
                .thenComparing(Comparator.comparing(GameInfo::getDiscountPercent).reversed()));

        // 한국어 지원 및 할인율 70% 이상인 게임만 필터링
        gameInfos.stream()
                .filter(game -> game.isSupportKorean() && game.getDiscountPercent() >= MIN_DISCOUNT_PERCENT)
                .forEach(game -> {
                    try {
                        saveHtml(filePath, game.toHtml(), true);
                    } catch (IOException e) {
                        log.error("HTML 저장 중 오류 발생: {}", game.getTitle(), e);
                    }
                });

        saveHtml(filePath, assembleEndHtml(), true);
    }

    private static String generateFilePath() {
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyyMMdd");
        return Paths.get("").toAbsolutePath() + "\\src\\html\\kor_" + LocalDateTime.now().format(dateFormat) + ".html";
    }

    private static void saveHtml(String filePath, String content, boolean append) throws IOException {
        try (OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream(filePath, append), StandardCharsets.UTF_8)) {
            writer.write(content);
        }
    }

    private static String assembleGuideHtml() {
        return "<p>70% 이상 할인이 적용된 게임만 선정하였습니다.</p>";
    }

    private static String assembleStartHtml() {
        return "<!DOCTYPE html>\n" +
               "<html lang=\"ko\">\n" +
               "<head>\n" +
               "    <meta charset=\"UTF-8\">\n" +
               "    <title>닌텐도 스위치 할인 정보</title>\n" +
               "</head>\n" +
               "<body>\n" +
               "<table style=\"border-collapse: collapse; width: 100%;\" border=\"1\" data-ke-align=\"alignLeft\">\n"
                + "<tbody>\n"
                + "<tr>\n"
                + "<td style=\"width: 20%;\">게임명</td>\n"
                + "<td style=\"width: 20%;\">게임정보</td>\n"
                + "<td style=\"width: 20%;\">할인기간</td>\n"
                + "<td style=\"width: 15%;\">할인가격</td>\n"
                + "<td style=\"width: 10%;\">한국어지원여부</td>\n"
                + "</tr>";
    }

    private static String assembleEndHtml() {
        return "</tbody>\n"
                + "</table>\n"
                + "</body>\n"
                + "</html>";
    }
}
