package com.weaone.themoa.domain.auth.repository;

import com.weaone.themoa.domain.auth.entity.MemberSocialAccount;
import com.weaone.themoa.domain.auth.entity.SocialProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberSocialAccountRepository extends JpaRepository<MemberSocialAccount, Long> {

    Optional<MemberSocialAccount> findByProviderAndProviderUserId(SocialProvider provider, String providerUserId);
}
