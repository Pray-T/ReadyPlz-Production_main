package io.readyplz.httpstatus;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 테스트 컨텍스트에서만 등록한다. 애플리케이션 스캔 패키지 밖에 두어 다른 테스트에 섞이지 않게 한다.
 */
@RestController
public class UnexpectedFailureController {

    public static final String PATH = "/api/auth/test-unexpected-error";
    public static final String LEAKED_DETAIL = "db connection leak";

    @GetMapping(PATH)
    public void fail() {
        throw new RuntimeException(LEAKED_DETAIL);
    }
}
