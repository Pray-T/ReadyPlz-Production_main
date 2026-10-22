package io.readyplz.readyplz.exception;

import io.readyplz.httpstatus.UnexpectedFailureController;
import io.readyplz.readyplz.service.EmailService;
import io.readyplz.readyplz.service.TokenService;
import io.readyplz.readyplz.support.TestRedisConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestRedisConfig.class, UnexpectedFailureController.class})
class RestApiExceptionHandlerTest {

    private static final String NOT_FOUND_MESSAGE = "요청한 리소스를 찾을 수 없습니다.";
    private static final String UNREADABLE_MESSAGE = "요청 본문을 확인할 수 없습니다.";
    private static final String MISSING_PARAM_MESSAGE = "필수 항목이 누락되었습니다.";
    private static final String METHOD_NOT_ALLOWED_MESSAGE = "허용되지 않은 메서드입니다.";
    private static final String SERVER_ERROR_MESSAGE = "서버 오류가 발생했습니다.";
    private static final String UNKNOWN_MSG_TEXT = "메시지를 표시할 수 없습니다.";
    private static final String UNKNOWN_ERROR_TEXT = "오류가 발생했습니다. 다시 시도해주세요.";

    private final RestApiExceptionHandler handler = new RestApiExceptionHandler();

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TokenService tokenService;

    @MockBean
    private EmailService emailService;

    @Test
    void handleIllegalArgument_returnsBadRequestWithoutInternalDetails() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleIllegalArgument(new IllegalArgumentException("duplicate_nickname"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("message", "duplicate_nickname");
    }

    @Test
    void handleGeneral_returnsGenericMessage() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleGeneral(new RuntimeException("db connection leak"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsOnlyKeys("message");
        assertThat(response.getBody()).containsEntry("message", SERVER_ERROR_MESSAGE);
        assertThat(response.getBody().get("message").toString()).doesNotContain("db connection leak", "RuntimeException");
    }

    @Test
    void handleNoResourceFound_returnsNotFoundWithoutInternalDetails() {
        NoResourceFoundException ex = new NoResourceFoundException(HttpMethod.GET, "css/missing.css");

        ResponseEntity<Map<String, Object>> response = handler.handleNoResourceFound(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsOnlyKeys("message");
        assertThat(response.getBody()).containsEntry("message", NOT_FOUND_MESSAGE);
        assertThat(response.getBody().get("message").toString())
                .doesNotContain("NoResourceFoundException", "No static resource", "css/missing.css");
    }

    @Test
    void handleMessageNotReadable_returnsBadRequestWithoutParserDetails() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
                "JSON parse error: Unexpected end-of-input",
                new MockHttpInputMessage("{\"username\":".getBytes(StandardCharsets.UTF_8)));

        ResponseEntity<Map<String, Object>> response = handler.handleMessageNotReadable(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsOnlyKeys("message");
        assertThat(response.getBody()).containsEntry("message", UNREADABLE_MESSAGE);
        assertThat(response.getBody().get("message").toString())
                .doesNotContain("HttpMessageNotReadableException", "JSON parse error");
    }

    @Test
    void handleMissingParameter_returnsBadRequestWithoutParameterDetails() {
        MissingServletRequestParameterException ex =
                new MissingServletRequestParameterException("token", "String");

        ResponseEntity<Map<String, Object>> response = handler.handleMissingParameter(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsOnlyKeys("message");
        assertThat(response.getBody()).containsEntry("message", MISSING_PARAM_MESSAGE);
        assertThat(response.getBody().get("message").toString())
                .doesNotContain("MissingServletRequestParameterException", "Required request parameter");
    }

    @Test
    void handleMethodNotSupported_returnsMethodNotAllowedWithoutMethodDetails() {
        HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException("GET");

        ResponseEntity<Map<String, Object>> response = handler.handleMethodNotSupported(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(response.getBody()).containsOnlyKeys("message");
        assertThat(response.getBody()).containsEntry("message", METHOD_NOT_ALLOWED_MESSAGE);
        assertThat(response.getBody().get("message").toString())
                .doesNotContain("HttpRequestMethodNotSupportedException", "Request method");
    }

    @Test
    void handleAuthentication_staysUnauthorized() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleAuthentication(new BadCredentialsException("bad password"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).containsEntry("message", "인증이 필요합니다.");
        assertThat(response.getBody().get("message").toString()).doesNotContain("bad password");
    }

    @Test
    void handleAccessDenied_staysForbidden() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleAccessDenied(new AccessDeniedException("hidden role"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).containsEntry("message", "접근 권한이 없습니다.");
        assertThat(response.getBody().get("message").toString()).doesNotContain("hidden role");
    }

    @Test
    void handleIllegalState_staysConflict() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleIllegalState(new IllegalStateException("duplicate account row"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).containsEntry("message", "요청을 처리할 수 없습니다.");
        assertThat(response.getBody().get("message").toString()).doesNotContain("duplicate account row");
    }

    @Test
    void missingStaticResource_isNotFoundWithoutExceptionText() throws Exception {
        mockMvc.perform(get("/css/does-not-exist-readyplz.css"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value(NOT_FOUND_MESSAGE))
                .andExpect(content().string(not(containsString("NoResourceFoundException"))))
                .andExpect(content().string(not(containsString("No static resource"))))
                .andExpect(content().string(not(containsString("does-not-exist-readyplz.css"))));
    }

    @Test
    void brokenLoginJson_isBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value(UNREADABLE_MESSAGE))
                .andExpect(content().string(not(containsString("HttpMessageNotReadableException"))))
                .andExpect(content().string(not(containsString("JSON parse error"))));
    }

    @Test
    void missingRequiredParameter_isBadRequest() throws Exception {
        mockMvc.perform(get("/members/reset-password"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value(MISSING_PARAM_MESSAGE))
                .andExpect(content().string(not(containsString("MissingServletRequestParameterException"))))
                .andExpect(content().string(not(containsString("Required request parameter"))));
    }

    @Test
    void unsupportedLoginMethod_isMethodNotAllowed() throws Exception {
        mockMvc.perform(get("/api/auth/login"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value(METHOD_NOT_ALLOWED_MESSAGE))
                .andExpect(content().string(not(containsString("HttpRequestMethodNotSupportedException"))))
                .andExpect(content().string(not(containsString("Request method"))));
    }

    @Test
    void unexpectedException_isGenericServerError() throws Exception {
        mockMvc.perform(get(UnexpectedFailureController.PATH))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value(SERVER_ERROR_MESSAGE))
                .andExpect(content().string(not(containsString(UnexpectedFailureController.LEAKED_DETAIL))))
                .andExpect(content().string(not(containsString("RuntimeException"))));
    }

    @Test
    void loginForm_withoutMsgCode_isHtml() throws Exception {
        mockMvc.perform(get("/members/loginForm"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("로그인")))
                .andExpect(model().attributeDoesNotExist("msg", "error"))
                .andExpect(content().string(not(containsString("NoSuchMessageException"))))
                .andExpect(content().string(not(containsString(SERVER_ERROR_MESSAGE))));
    }

    @Test
    void loginForm_unknownMsgCode_isHtmlWithDefaultText() throws Exception {
        mockMvc.perform(get("/members/loginForm").param("msgCode", "nope"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("로그인")))
                .andExpect(model().attribute("msg", UNKNOWN_MSG_TEXT))
                .andExpect(content().string(containsString(UNKNOWN_MSG_TEXT)))
                .andExpect(content().string(not(containsString("NoSuchMessageException"))))
                .andExpect(content().string(not(containsString(SERVER_ERROR_MESSAGE))));
    }

    @Test
    void loginForm_knownMsgCode_keepsResolvedText() throws Exception {
        mockMvc.perform(get("/members/loginForm").param("msgCode", "login.success"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(model().attribute("msg", "성공적으로 로그인했습니다."))
                .andExpect(content().string(containsString("성공적으로 로그인했습니다.")));
    }

    @Test
    void loginForm_unknownErrorCode_isHtmlWithDefaultText() throws Exception {
        mockMvc.perform(get("/members/loginForm").param("errorCode", "nope"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(model().attribute("error", UNKNOWN_ERROR_TEXT))
                .andExpect(content().string(containsString("로그인")))
                .andExpect(content().string(not(containsString("NoSuchMessageException"))))
                .andExpect(content().string(not(containsString(SERVER_ERROR_MESSAGE))));
    }

    @Test
    void loginForm_knownErrorCode_keepsResolvedText() throws Exception {
        mockMvc.perform(get("/members/loginForm").param("errorCode", "login.error"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("error", "아이디 또는 비밀번호가 올바르지 않습니다."));
    }

    @Test
    void loginValidation_staysBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"secret1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("아이디를 입력해주세요."))
                .andExpect(content().string(not(containsString(SERVER_ERROR_MESSAGE))))
                .andExpect(content().string(not(containsString(UNREADABLE_MESSAGE))));
    }

    @Test
    void loginBadCredentials_staysUnauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"missing-user\",\"password\":\"password1\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("아이디 또는 비밀번호가 일치하지 않습니다."))
                .andExpect(content().string(not(containsString("사용자를 찾을 수 없습니다"))))
                .andExpect(content().string(not(containsString(SERVER_ERROR_MESSAGE))));
    }
}
