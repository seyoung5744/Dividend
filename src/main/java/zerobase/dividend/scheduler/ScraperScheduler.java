package zerobase.dividend.scheduler;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import zerobase.dividend.model.Company;
import zerobase.dividend.model.ScrapedResult;
import zerobase.dividend.model.constants.CacheKey;
import zerobase.dividend.repository.CompanyRepository;
import zerobase.dividend.repository.DividendRepository;
import zerobase.dividend.repository.entity.CompanyEntity;
import zerobase.dividend.repository.entity.DividendEntity;
import zerobase.dividend.scraper.Scraper;

@Slf4j
@Component
@EnableCaching
@RequiredArgsConstructor
public class ScraperScheduler {

    private final CompanyRepository companyRepository;
    private final DividendRepository dividendRepository;

    private final Scraper yahooFinanceScraper;

    // 일정 주기마다 수행
    @CacheEvict(value = CacheKey.KEY_FINANCE, allEntries = true) // value값이 key의 prefix로 사용됨. allEntries = true : redis의 finance에 해당하는 값은 모두 비우겠다.
    @Scheduled(cron = "${scheduler.scrap.yahoo}")
    public void yahooFinanceScheduling() {
        log.info("scraping scheduler is started");

        // 저장된 회사 목록을 조회
        // TODO: 회사 수가 많아지면 bulk로 가져올지도 고려사항 => Spring Batch로 bulk 데이터 처리 관련해서 작업처리 통계, loggin 처리 등... 많은 수의 대용량 처리에 있어서 유용한 기능을 제공
        List<CompanyEntity> companies = this.companyRepository.findAll();

        // 회사마다 배당금 정보를 새로 스크래핑
        for (var company : companies) {
            log.info("scraping scheduler is started -> " + company.getName());
            ScrapedResult scrapedResult = this.yahooFinanceScraper.scrap(Company.builder()
                .name(company.getName())
                .ticker(company.getTicker())
                .build());

            // 스크래핑한 배당금 정보 중 DB에 없는 값은 저장 - 중복 저장을 막기 위해 복합 unique key 설정
            scrapedResult.getDividendEntities().stream()
                // 모델을 엔티티로 매핑
                .map(e -> new DividendEntity(company.getId(), e))
                // 엘리먼트를 하나씩 레퍼지토리에 삽입
                .forEach(e -> {
                    boolean exists = this.dividendRepository.existsByCompanyIdAndDate(e.getCompanyId(), e.getDate());
                    if (!exists) {
                        this.dividendRepository.save(e);
                        log.info("insert new dividend -> " + e.toString());
                    }
                });

            // 연속적으로 스크래핑 대상 사이트 서버에 요청을 날리지 않도록 일시정지
            try {
                Thread.sleep(3000); // 3 seconds
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
