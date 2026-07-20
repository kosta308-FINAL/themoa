package com.weaone.themoa.domain.category.repository;

import com.weaone.themoa.domain.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByCode(String code);

    /** 거래 입력·수정용 카테고리 목록(dayguide.md §8.1). 시드 순서 = 화면 표시 순서라 id 오름차순으로 고정한다. */
    List<Category> findAllByOrderByIdAsc();
}
