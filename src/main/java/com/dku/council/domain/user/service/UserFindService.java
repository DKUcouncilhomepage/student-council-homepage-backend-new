package com.dku.council.domain.user.service;

import com.dku.council.domain.user.exception.NotSMSAuthorizedException;
import com.dku.council.domain.user.exception.NotSMSSentException;
import com.dku.council.domain.user.exception.WrongSMSCodeException;
import com.dku.council.domain.user.model.SMSAuth;
import com.dku.council.domain.user.model.dto.response.ResponsePasswordChangeTokenDto;
import com.dku.council.domain.user.model.entity.User;
import com.dku.council.domain.user.repository.UserFindRepository;
import com.dku.council.domain.user.repository.UserRepository;
import com.dku.council.domain.user.util.CodeGenerator;
import com.dku.council.global.error.exception.UserNotFoundException;
import com.dku.council.infra.nhn.service.SMSService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class UserFindService {

    public static final String CODE_AUTH_COMPLETED = "OK";

    private final Clock clock;
    private final SMSService smsService;
    private final UserRepository userRepository;
    private final UserFindRepository userFindRepository;
    private final MessageSource messageSource;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.auth.sms.digit-count}")
    private final int digitCount;


    @Transactional(readOnly = true)
    public void sendIdBySMS(String phone) {
        phone = eliminateDash(phone);
        User user = userRepository.findByPhone(phone).orElseThrow(UserNotFoundException::new);
        String studentId = user.getStudentId();
        sendSMS(phone, "sms.find.id-message", studentId);
    }

    @Transactional(readOnly = true)
    public ResponsePasswordChangeTokenDto sendPwdCodeBySMS(String studentId, String phone) {
        Instant now = Instant.now(clock);
        String code = CodeGenerator.generateDigitCode(digitCount);
        User user = userRepository.findByStudentId(studentId).orElseThrow(UserNotFoundException::new);

        phone = eliminateDash(phone);
        if (!user.getPhone().equals(phone)) {
            throw new UserNotFoundException();
        }

        String token = CodeGenerator.generateUUIDCode();
        userFindRepository.setPwdAuthCode(token, code, phone, now);
        sendSMS(phone, "sms.find.pwd-auth-message", code);

        return new ResponsePasswordChangeTokenDto(token);
    }

    public void verifyPwdCode(String token, String code) {
        Instant now = Instant.now(clock);

        SMSAuth auth = userFindRepository.getPwdAuthCode(token, now)
                .orElseThrow(NotSMSSentException::new);

        if (!auth.getCode().equals(code)) {
            throw new WrongSMSCodeException();
        }

        userFindRepository.setPwdAuthCode(token, CODE_AUTH_COMPLETED, auth.getPhone(), now);
    }

    @Transactional
    public void changePassword(String token, String password) {
        Instant now = Instant.now(clock);
        SMSAuth auth = userFindRepository.getPwdAuthCode(token, now)
                .orElseThrow(NotSMSSentException::new);

        if (!auth.getCode().equals(CODE_AUTH_COMPLETED)) {
            throw new NotSMSAuthorizedException();
        }

        User user = userRepository.findByPhone(auth.getPhone()).orElseThrow(UserNotFoundException::new);
        password = passwordEncoder.encode(password);
        user.changePassword(password);

        userFindRepository.deletePwdAuthCode(token);
    }

    private void sendSMS(String phone, String messageCode, String argument) {
        Locale locale = LocaleContextHolder.getLocale();
        smsService.sendSMS(phone, messageSource.getMessage(messageCode, new Object[]{argument}, locale));
    }

    private static String eliminateDash(String phone) {
        return phone.replaceAll("-", "");
    }
}