package com.weaone.themoa.domain.auth.repository;

import com.weaone.themoa.domain.auth.entity.MemberSocialAccount;
import com.weaone.themoa.domain.auth.entity.SocialProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemberSocialAccountRepository extends JpaRepository<MemberSocialAccount, Long> {

    Optional<MemberSocialAccount> findByProviderAndProviderUserId(SocialProvider provider, String providerUserId);

    long deleteByMember_Id(Long memberId);

    /** 관리자 회원 상세 화면에 연동된 소셜 제공자만 보여주기 위한 조회(providerUserId는 노출하지 않는다). */
    List<MemberSocialAccount> findByMember_Id(Long memberId);
}
