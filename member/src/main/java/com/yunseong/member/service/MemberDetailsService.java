package com.yunseong.member.service;

import com.yunseong.member.domain.Member;
import com.yunseong.member.domain.MemberRepository;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;

@Service
@Transactional
@AllArgsConstructor
public class MemberDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException, CannotLoginBecauseNotAuthenticatedException {
        Member member = this.memberRepository.findByUsername(username).orElseThrow(() -> new EntityNotFoundException("해당 유저는 존재하지않습니다."));
        if(!member.isAuthenticated()) throw new CannotLoginBecauseNotAuthenticatedException("해당 계정은 아직 이메일인증을 거치지 않은 계정이므로 이용이 불가능합니다.");
        return new MyUser(member);
    }
}
