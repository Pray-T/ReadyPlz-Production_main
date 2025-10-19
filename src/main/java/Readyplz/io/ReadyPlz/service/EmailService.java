package Readyplz.io.ReadyPlz.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import jakarta.mail.internet.MimeMessage;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    private final JavaMailSender mailSender;

    @Value("${app.external.base-url}")
    private String appBaseUrl;

    @Async("mailTaskExecutor")
    public void sendResetMail(String to, String token) {
        try {
            String link = appBaseUrl + "/members/reset-password?token=" + token;
            String subject = "[ReadyPlz] 비밀번호 재설정 안내";
            String html = "<div style=\"font-family:Arial,AppleSDGothicNeo,Malgun Gothic,sans-serif;font-size:14px;line-height:1.6;\">"
                + "<p>아래 링크를 클릭하여 비밀번호를 재설정하세요.</p>"
                + "<p><a href=\"" + link + "\" target=\"_blank\" style=\"color:#0d6efd;text-decoration:underline;\">비밀번호 재설정</a></p>"
                + "<p style=\"margin-top:12px;color:#666;\">만약 위 링크가 보이지 않으면 다음 URL을 브라우저 주소창에 복사하여 접속하세요:<br>"
                + "<span style=\"word-break:break-all;\">" + link + "</span></p>"
                + "<hr style=\"border:none;border-top:1px solid #eee;margin:16px 0;\">"
                + "<p style=\"color:#888;\">이 메일을 요청하지 않으셨다면 무시하셔도 됩니다.</p>"
                + "</div>";

            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true); // HTML 본문 사용
            mailSender.send(mime);
            log.info("비밀번호 재설정 메일 발송 완료: {}", to);
        } catch (Exception e) {
            log.error("비밀번호 재설정 메일 발송 실패: {} - {}", to, e.getMessage());
        }
    }
}