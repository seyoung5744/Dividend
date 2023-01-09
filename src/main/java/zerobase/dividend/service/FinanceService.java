package zerobase.dividend.service;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import zerobase.dividend.model.Company;
import zerobase.dividend.model.Dividend;
import zerobase.dividend.model.ScrapedResult;
import zerobase.dividend.model.constants.CacheKey;
import zerobase.dividend.repository.CompanyRepository;
import zerobase.dividend.repository.DividendRepository;
import zerobase.dividend.repository.entity.CompanyEntity;
import zerobase.dividend.repository.entity.DividendEntity;

@Slf4j
@Service
@RequiredArgsConstructor
public class FinanceService {

    private final CompanyRepository companyRepository;
    private final DividendRepository dividendRepository;

    // 요청이 자주 들어오는가?
    // 자주 변경되는 데이터 인가?
    @Cacheable(key = "#companyName", value = CacheKey.KEY_FINANCE) // redis의 key-value와 다른 의미
    public ScrapedResult getDividendByCompanyName(String companyName) {
        log.info("search Company -> " + companyName);
        // 1. 회사명을 기준으로 회사 정보 조회
        CompanyEntity company = this.companyRepository.findByName(companyName)
            .orElseThrow(() -> new RuntimeException("존재하지 않는 회사명입니다."));

        // 2. 조회된 회사 ID로 배당금 정보 조회
        List<DividendEntity> dividendEntities = this.dividendRepository.findAllByCompanyId(company.getId());

        // 3. 결과 조합 후 반환
        List<Dividend> dividends = dividendEntities.stream()
            .map(e -> Dividend.builder()
                .date(e.getDate())
                .dividend(e.getDividend())
                .build())
            .collect(Collectors.toList());

        return new ScrapedResult(
            Company.builder()
                .ticker(company.getTicker())
                .name(company.getName())
                .build()
            , dividends
        );
    }

}
