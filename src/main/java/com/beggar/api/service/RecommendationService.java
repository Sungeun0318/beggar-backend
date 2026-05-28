package com.beggar.api.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RecommendationService {

    // TODO: recommend(roomNo) — aiServerWebClient 로 Python AI 서버 GET 호출
    //                          위치 + 예산 + 태그를 쿼리로 전달
    //                          응답을 RecommendationResponse 로 매핑해 반환 (DB 미적재)
}
