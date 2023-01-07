package zerobase.dividend.config;

import org.apache.commons.collections4.Trie;
import org.apache.commons.collections4.trie.PatriciaTrie;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    // Service에서 매번 new 를 해줘도 되지만...(@Service로 인해 싱글톤으로 관리됨)
    // 코드 일관성을 위해 Bean으로 등록
    @Bean
    public Trie<String, String> trie() {
        return new PatriciaTrie<>();
    }
}
