package org.example.p2pfileshare.service;

import org.example.p2pfileshare.model.SearchResult;

import java.util.ArrayList;
import java.util.List;

public class SearchService {

    /** Tìm kiếm file (hiện tại demo, sau sẽ gọi UDP broadcast) */
    public List<SearchResult> search(String keyword) {

        // TODO: gửi gói SEARCH qua UDP (nếu bạn muốn)
        // TODO: nhận phản hồi JSON về danh sách file
        // TODO: parse vào SearchResult

        System.out.println("[SearchService] Search keyword = " + keyword);

        // trả về list rỗng tạm thời (để controller chạy OK)
        return new ArrayList<>();
    }
}
