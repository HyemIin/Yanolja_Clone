package com.example.miniproject.domain.accommodation.service;

import com.example.miniproject.domain.accommodation.dto.request.AccommodationRegisterRequest;
import com.example.miniproject.domain.accommodation.dto.response.AccommodationRegisterResponse;

public interface AccommodationService {

    AccommodationRegisterResponse register(AccommodationRegisterRequest request);
}