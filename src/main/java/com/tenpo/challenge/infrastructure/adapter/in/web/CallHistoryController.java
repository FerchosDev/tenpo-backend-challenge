package com.tenpo.challenge.infrastructure.adapter.in.web;

import com.tenpo.challenge.infrastructure.adapter.out.persistence.CallHistoryReader;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CallHistoryController {

    private final CallHistoryReader callHistoryReader;

    @GetMapping("/history")
    public Page<CallHistoryResponse> getHistory(@PageableDefault(size = 20) Pageable pageable) {
        Pageable sortedByTimestampDesc = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "timestamp")
        );
        return callHistoryReader.findAll(sortedByTimestampDesc).map(CallHistoryResponse::from);
    }

}
