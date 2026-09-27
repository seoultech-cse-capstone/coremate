package com.coremate.backend.global.mail;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    public void sendVerificationCode(String toEmail, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("[CoreMate] 비밀번호 재설정 인증번호");
        message.setText(
                "비밀번호 재설정 인증번호는 [" + code + "] 입니다.\n"
                        + "인증번호는 5분간 유효하며, 타인에게 노출되지 않도록 주의해 주세요.\n"
                        + "본인이 요청하지 않았다면 이 메일을 무시하셔도 됩니다."
        );
        mailSender.send(message);
    }

    public void sendSignupVerificationCode(String toEmail, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("[CoreMate] 회원가입 이메일 인증번호");
        message.setText(
                "회원가입 이메일 인증번호는 [" + code + "] 입니다.\n"
                        + "인증번호는 5분간 유효하며, 타인에게 노출되지 않도록 주의해 주세요.\n"
                        + "본인이 요청하지 않았다면 이 메일을 무시하셔도 됩니다."
        );
        mailSender.send(message);
    }
}
