package com.example.miniproject.domain.member.service;


import com.example.miniproject.domain.member.dto.request.MemberSignUpRequest;
import com.example.miniproject.domain.member.dto.response.MemberSignUpResponse;

public interface MemberService {

    MemberSignUpResponse signUp(MemberSignUpRequest request);
}
