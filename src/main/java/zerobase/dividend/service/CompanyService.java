package zerobase.dividend.service;

import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.apache.commons.collections4.Trie;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import zerobase.dividend.model.Company;
import zerobase.dividend.model.ScrapedResult;
import zerobase.dividend.repository.CompanyRepository;
import zerobase.dividend.repository.DividendRepository;
import zerobase.dividend.repository.entity.CompanyEntity;
import zerobase.dividend.repository.entity.DividendEntity;
import zerobase.dividend.scraper.Scraper;

@Service
@AllArgsConstructor
public class CompanyService {

    // Config에 의해 Spring으로 관리되는 Bean으로 생성된 Trie Bean이 초기화될때 Service에 주입이되면서
    // 해당 Service의 Trie 인스턴스로 사용됨.
    private final Trie trie;

    private final Scraper yahooFinanceScraper;

    private final CompanyRepository companyRepository;
    private final DividendRepository dividendRepository;

    public Company save(String ticker) {
        // 회사 존재 여부 확인
        boolean exists = this.companyRepository.existsByTicker(ticker);

        if (exists) {
            throw new RuntimeException("already exitsts ticker -> " + ticker);
        }

        return this.storeCompanyAndDividend(ticker);
    }

    public Page<CompanyEntity> getAllCompany(Pageable pageable) {
        return this.companyRepository.findAll(pageable);
    }

    public List<String> getCompanyNamesByKeyword(String keyword) {

        Pageable limit = PageRequest.of(0, 10);

        Page<CompanyEntity> companyEntities = this.companyRepository.findByNameStartingWithIgnoreCase(keyword, limit);

        return companyEntities.stream()
            .map(e -> e.getName())
            .collect(Collectors.toList());
    }


    private Company storeCompanyAndDividend(String ticker) {
        // ticker 를 기준으로 회사를 스크래핑
        Company company = this.yahooFinanceScraper.scrapCompanyByTicker(ticker);

        if (ObjectUtils.isEmpty(company)) {
            throw new RuntimeException("Failed to scrap ticker -> " + ticker);
        }

        // 해당 회가사 존재할 경우, 회사의 배당금 정보를 스크래핑
        ScrapedResult scrapedResult = this.yahooFinanceScraper.scrap(company);

        // 스크래핑 결과
        CompanyEntity companyEntity = this.companyRepository.save(new CompanyEntity(company));
        List<DividendEntity> dividendEntities = scrapedResult.getDividendEntities().stream()
            .map(e -> new DividendEntity(companyEntity.getId(), e))
            .collect(Collectors.toList());

        this.dividendRepository.saveAll(dividendEntities);
        return company;
    }

    public void addAutoCompleteKeyword(String keyword) {
        // apach Trie는 추가 기능이 내장되어 있어서 key:value로 사용가능
        // 하지만 프로젝트에서는 자동완성을 위해 사용되기 때문에 key 값만 저장
        this.trie.put(keyword, null);
    }

    public List<String> autoComplete(String keyword) {
        return (List<String>) this.trie.prefixMap(keyword).keySet()
            .stream()
            .limit(10)
            .collect(Collectors.toList());
    }

    public void deleteAutoCompleteKeyword(String keyword) {
        this.trie.remove(keyword);
    }

    /**
     * ticket에 해당하는 회사 삭제 후 회사명 반환
     */
    public String deleteCompany(String ticker) {
        CompanyEntity company = this.companyRepository.findByTicker(ticker)
            .orElseThrow(() -> new RuntimeException("존재하지 않는 회사입니다."));

        // ticker에 해당하는 배당금 정보 삭제
        this.dividendRepository.deleteAllByCompanyId(company.getId());

        // 회사 삭제
        this.companyRepository.delete(company);

        // 자동 완성을 위한 trie의 회사명 삭제
        this.deleteAutoCompleteKeyword(company.getName());

        return company.getName();
    }
}
