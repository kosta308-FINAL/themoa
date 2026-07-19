package com.weaone.themoa.domain.category.service;

import com.weaone.themoa.domain.category.entity.Category;
import com.weaone.themoa.domain.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 활성 전역 카테고리 조회(dayguide.md §8.1). 거래 입력·수정 화면의 카테고리 선택지로 쓰인다. */
@Service
@RequiredArgsConstructor
public class CategoryQueryService {

    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<Category> listAll() {
        return categoryRepository.findAllByOrderByIdAsc();
    }
}
