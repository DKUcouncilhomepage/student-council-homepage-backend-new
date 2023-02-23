package com.dku.council.domain.user.service;

import com.dku.council.domain.user.exception.NotDKUAuthorizedException;
import com.dku.council.domain.user.model.dto.request.RequestVerifyStudentDto;
import com.dku.council.domain.user.model.dto.response.ResponseVerifyStudentDto;
import com.dku.council.domain.user.repository.DKUAuthRepository;
import com.dku.council.infra.dku.model.StudentInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DKUAuthService {

    private final DKUAuthRepository repository;

    /**
     * 회원가입 토큰을 기반으로 인증된 학생 정보를 가져옵니다. 학생인증이 되어있지 않으면 Exception이 발생합니다.
     * 이 메서드는 회원가입 진행자를 대상으로 합니다.
     *
     * @param signupToken 회원가입 토큰
     * @throws NotDKUAuthorizedException 학생 인증을 하지 않았을 경우
     */
    public StudentInfo getStudentInfo(String signupToken) throws NotDKUAuthorizedException {
        // TODO Implementation
        return null;
    }

    /**
     * 단대 id, password로 학생 인증을 진행합니다.
     *
     * @param dto 요청 dto
     * @return 학생 인증 결과 dto
     */
    public ResponseVerifyStudentDto verifyStudent(RequestVerifyStudentDto dto) {
        // TODO Implementation
        return null;
    }
}
