package com.cpt202.HerLink.service.impl;

import com.cpt202.HerLink.entity.Category;
import com.cpt202.HerLink.mapper.CategoryMapper;
import com.cpt202.HerLink.service.CategoryService;
import com.cpt202.HerLink.vo.CategoryTagOptionVO;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;

    public CategoryServiceImpl(CategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    @Override
    public List<CategoryTagOptionVO> listCategoryOptions() {
        List<Category> categoryList = categoryMapper.selectActiveCategories();
        if (categoryList == null) {
            categoryList = Collections.emptyList();
        }

        List<CategoryTagOptionVO> optionVOList = new ArrayList<>();
        for (Category category : categoryList) {
            CategoryTagOptionVO optionVO = new CategoryTagOptionVO();
            optionVO.setId(category.getCategoryId());
            optionVO.setName(category.getCategoryTopic());
            optionVOList.add(optionVO);
        }

        return optionVOList;
    }
}
